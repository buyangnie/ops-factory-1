import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { realpath, stat } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { logError, logInfo } from './logger.js';

const DEFAULT_ALLOWED_HOSTS = ['localhost', '127.0.0.1'];
const DEFAULT_COMMANDS = ['pwd', 'ls', 'rg', 'cat', 'head', 'tail', 'wc', 'ps', 'top', 'lsof', 'df', 'du'];
const FETCH_TIMEOUT_MS = parsePositiveInt(process.env.LOCAL_TINY_FETCH_TIMEOUT_MS, 8000, 1000, 30000);
const FETCH_MAX_BYTES = parsePositiveInt(process.env.LOCAL_TINY_FETCH_MAX_BYTES, 65536, 1024, 262144);
const COMMAND_TIMEOUT_MS = parsePositiveInt(process.env.LOCAL_TINY_COMMAND_TIMEOUT_MS, 5000, 1000, 30000);
const COMMAND_MAX_OUTPUT_BYTES = parsePositiveInt(process.env.LOCAL_TINY_MAX_OUTPUT_BYTES, 65536, 1024, 262144);
const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../../../../../../..');
const DANGEROUS_ARG_PATTERN = /[|;&<>`]/;
const COMMANDS_WITH_LIMITED_PATHS = new Set(['rg', 'cat', 'head', 'tail', 'wc', 'du']);

type ToolArgs = Record<string, unknown>;
type ToolError = { code: string; message: string } | null;

type ToolResultPayload = {
  tool: string;
  ok: boolean;
  summary: string;
  data?: unknown;
  truncated?: boolean;
  warnings?: string[];
  error?: ToolError;
};

type TextLimitResult = {
  text: string;
  truncated: boolean;
  bytes: number;
};

type CwdResult = {
  ok: true;
  cwd: string;
  roots: string[];
} | {
  ok: false;
  error: string;
  roots: string[];
};

type CommandInput = {
  command: string;
  args: string[];
};

type CommandOutput = {
  stdout: string;
  stderr: string;
  truncated: boolean;
};

type CommandResult = CommandOutput & {
  exitCode: number | null;
  signal?: NodeJS.Signals | null;
  timedOut: boolean;
  error: Error | null;
};

export const tools = Object.freeze([
  {
    name: 'fetch_url_content',
    description: 'Fetch text or JSON content from an allowed local URL. Only GET is supported.',
    inputSchema: {
      type: 'object',
      properties: {
        url: {
          type: 'string',
          description: 'URL to fetch. localhost:3000/path is accepted and normalized to http://localhost:3000/path.',
        },
        max_bytes: {
          type: 'number',
          description: 'Optional response byte limit. Defaults to the server limit.',
          minimum: 1024,
          maximum: FETCH_MAX_BYTES,
        },
      },
      required: ['url'],
    },
  },
  {
    name: 'run_command',
    description: 'Run a whitelisted local command with bounded arguments, cwd, timeout, and output.',
    inputSchema: {
      type: 'object',
      properties: {
        command: {
          type: 'string',
          description: 'Command name, or a simple command string such as "ls -la".',
        },
        args: {
          type: ['array', 'string'],
          description: 'Arguments as an array or a simple string. Shell operators are rejected.',
          items: { type: 'string' },
        },
        cwd: {
          type: 'string',
          description: 'Working directory. Defaults to the MCP process working directory.',
        },
        timeout_ms: {
          type: 'number',
          description: 'Optional timeout in milliseconds.',
          minimum: 1000,
          maximum: COMMAND_TIMEOUT_MS,
        },
        max_output_bytes: {
          type: 'number',
          description: 'Optional stdout/stderr byte limit.',
          minimum: 1024,
          maximum: COMMAND_MAX_OUTPUT_BYTES,
        },
      },
      required: ['command'],
    },
  },
] as const);

function parsePositiveInt(raw: unknown, fallback: number, min: number, max: number): number {
  const parsed = Number.parseInt(String(raw ?? ''), 10);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}

function normalizeString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function splitSimpleCommand(input: string): string[] {
  const parts: string[] = [];
  let current = '';
  let quote: string | null = null;

  for (const char of input) {
    if ((char === '"' || char === "'") && quote === null) {
      quote = char;
      continue;
    }
    if (char === quote) {
      quote = null;
      continue;
    }
    if (/\s/.test(char) && quote === null) {
      if (current) {
        parts.push(current);
        current = '';
      }
      continue;
    }
    current += char;
  }

  if (current) parts.push(current);
  return parts;
}

function normalizeArgs(args: unknown): string[] {
  if (Array.isArray(args)) {
    return args.map((arg) => String(arg)).filter((arg) => arg.length > 0);
  }
  if (typeof args === 'string') {
    return splitSimpleCommand(args);
  }
  return [];
}

function makeResult({
  tool,
  ok,
  summary,
  data = null,
  truncated = false,
  warnings = [],
  error = null,
}: ToolResultPayload): string {
  return JSON.stringify({
    ok,
    tool,
    summary,
    data,
    truncated,
    warnings,
    error,
  }, null, 2);
}

function errorResult(tool: string, code: string, message: string, warnings: string[] = []): string {
  return makeResult({
    tool,
    ok: false,
    summary: message,
    warnings,
    error: { code, message },
  });
}

function allowedHosts(): string[] {
  return (process.env.LOCAL_TINY_ALLOWED_HOSTS || DEFAULT_ALLOWED_HOSTS.join(','))
    .split(',')
    .map((host) => host.trim().toLowerCase())
    .filter(Boolean);
}

function allowedCommands(): string[] {
  return (process.env.LOCAL_TINY_ALLOWED_COMMANDS || DEFAULT_COMMANDS.join(','))
    .split(',')
    .map((command) => command.trim())
    .filter(Boolean);
}

function normalizeUrl(rawUrl: unknown): URL | null {
  const trimmed = normalizeString(rawUrl);
  if (!trimmed) return null;
  const withScheme = /^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed) ? trimmed : `http://${trimmed}`;
  try {
    return new URL(withScheme);
  } catch {
    return null;
  }
}

function isReadableContentType(contentType: string): boolean {
  const normalized = contentType.toLowerCase();
  return normalized.startsWith('text/')
    || normalized.includes('json')
    || normalized.includes('xml')
    || normalized.includes('yaml')
    || normalized.includes('javascript');
}

function truncateText(text: string, maxBytes: number): TextLimitResult {
  const buffer = Buffer.from(text, 'utf8');
  if (buffer.length <= maxBytes) {
    return { text, truncated: false, bytes: buffer.length };
  }
  return {
    text: buffer.subarray(0, maxBytes).toString('utf8'),
    truncated: true,
    bytes: buffer.length,
  };
}

async function responseTextWithLimit(response: Response, maxBytes: number): Promise<TextLimitResult> {
  if (!response.body) {
    const text = await response.text();
    return truncateText(text, maxBytes);
  }

  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let bytes = 0;
  let truncated = false;

  try {
    while (bytes < maxBytes) {
      const { done, value } = await reader.read();
      if (done) break;

      if (bytes + value.byteLength > maxBytes) {
        chunks.push(value.slice(0, maxBytes - bytes));
        bytes += value.byteLength;
        truncated = true;
        await reader.cancel();
        break;
      }

      chunks.push(value);
      bytes += value.byteLength;
    }
  } finally {
    reader.releaseLock();
  }

  return {
    text: Buffer.concat(chunks).toString('utf8'),
    truncated,
    bytes,
  };
}

export async function handleFetchUrlContent(args: ToolArgs): Promise<string> {
  const tool = 'fetch_url_content';
  const url = normalizeUrl(args.url);
  if (!url) {
    return errorResult(tool, 'URL_REQUIRED', 'A valid url string is required.');
  }

  if (!['http:', 'https:'].includes(url.protocol)) {
    return errorResult(tool, 'URL_SCHEME_NOT_ALLOWED', 'Only http and https URLs are allowed.');
  }

  if (!allowedHosts().includes(url.hostname.toLowerCase())) {
    return errorResult(
      tool,
      'HOST_NOT_ALLOWED',
      `Host ${url.hostname} is not allowed. Allowed hosts: ${allowedHosts().join(', ')}.`,
    );
  }

  const maxBytes = parsePositiveInt(args.max_bytes, FETCH_MAX_BYTES, 1024, FETCH_MAX_BYTES);
  const startedAt = Date.now();
  logInfo('fetch_started', { url: url.toString(), maxBytes });

  try {
    const response = await fetch(url, {
      method: 'GET',
      signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
      redirect: 'follow',
    });
    const finalUrl = new URL(response.url);
    if (!allowedHosts().includes(finalUrl.hostname.toLowerCase())) {
      return errorResult(
        tool,
        'REDIRECT_HOST_NOT_ALLOWED',
        `Redirect target host ${finalUrl.hostname} is not allowed.`,
      );
    }
    const contentType = response.headers.get('content-type') || '';

    if (!response.ok) {
      const body = await responseTextWithLimit(response, Math.min(maxBytes, 4096)).catch(() => ({
        text: '',
        truncated: false,
        bytes: 0,
      }));
      return makeResult({
        tool,
        ok: false,
        summary: `URL returned HTTP ${response.status}.`,
        data: {
          url: finalUrl.toString(),
          requested_url: url.toString(),
          status: response.status,
          content_type: contentType,
          body: body.text,
        },
        truncated: body.truncated,
        error: {
          code: 'HTTP_ERROR',
          message: `URL returned HTTP ${response.status}.`,
        },
      });
    }

    if (!isReadableContentType(contentType)) {
      return makeResult({
        tool,
        ok: false,
        summary: `Content type ${contentType || 'unknown'} is not readable as text.`,
        data: {
          url: finalUrl.toString(),
          requested_url: url.toString(),
          status: response.status,
          content_type: contentType || null,
        },
        error: {
          code: 'CONTENT_TYPE_NOT_READABLE',
          message: 'The response is not text, JSON, XML, YAML, or JavaScript.',
        },
      });
    }

    const body = await responseTextWithLimit(response, maxBytes);
    return makeResult({
      tool,
      ok: true,
      summary: `Fetched ${url.toString()} with HTTP ${response.status}.`,
      data: {
        url: finalUrl.toString(),
        requested_url: url.toString(),
        status: response.status,
        content_type: contentType || null,
        bytes: body.bytes,
        body: body.text,
      },
      truncated: body.truncated,
      warnings: body.truncated ? [`Response exceeded ${maxBytes} bytes and was truncated.`] : [],
    });
  } catch (error) {
    logError('fetch_failed', { url: url.toString(), durationMs: Date.now() - startedAt, error });
    return errorResult(tool, 'FETCH_FAILED', error instanceof Error ? error.message : String(error));
  }
}

async function allowedRoots(): Promise<string[]> {
  const roots = (process.env.LOCAL_TINY_COMMAND_ROOTS || `${process.cwd()},${REPO_ROOT}`)
    .split(',')
    .map((root) => root.trim())
    .filter(Boolean);

  const realRoots: string[] = [];
  for (const root of roots) {
    try {
      if (existsSync(root)) {
        realRoots.push(await realpath(root));
      }
    } catch {
      // Ignore invalid configured roots.
    }
  }
  return [...new Set(realRoots)];
}

async function normalizeCwd(rawCwd: unknown): Promise<CwdResult> {
  const requested = normalizeString(rawCwd) || process.cwd();
  const absolute = resolve(requested);
  const roots = await allowedRoots();

  try {
    const realCwd = await realpath(absolute);
    const info = await stat(realCwd);
    if (!info.isDirectory()) {
      return { ok: false, error: `cwd is not a directory: ${requested}`, roots };
    }
    const inAllowedRoot = roots.some((root) => realCwd === root || realCwd.startsWith(`${root}/`));
    if (!inAllowedRoot) {
      return { ok: false, error: `cwd is outside allowed roots: ${requested}`, roots };
    }
    return { ok: true, cwd: realCwd, roots };
  } catch {
    return { ok: false, error: `cwd does not exist: ${requested}`, roots };
  }
}

function normalizeCommandInput(args: ToolArgs): CommandInput {
  const commandInput = normalizeString(args.command);
  if (!commandInput) return { command: '', args: [] };

  const commandParts = splitSimpleCommand(commandInput);
  const command = commandParts[0] || '';
  const inlineArgs = commandParts.slice(1);
  const explicitArgs = normalizeArgs(args.args);
  return { command, args: [...inlineArgs, ...explicitArgs] };
}

function rejectUnsafeArgs(commandArgs: string[]): string | null {
  for (const arg of commandArgs) {
    if (DANGEROUS_ARG_PATTERN.test(arg) || arg.includes('$(')) {
      return `Argument contains a shell operator or unsafe expansion: ${arg}`;
    }
    if (arg.includes('\u0000') || arg.includes('\n') || arg.includes('\r')) {
      return 'Arguments may not contain control characters or newlines.';
    }
  }
  return null;
}

function normalizeCommandArgs(command: string, commandArgs: string[]): string[] {
  if (command === 'top') {
    if (process.platform === 'darwin') {
      return ['-l', '1', '-n', '20'];
    }
    return ['-b', '-n', '1'];
  }

  if (command === 'rg') {
    const hasCountLimit = commandArgs.some((arg) => arg === '-m' || arg === '--max-count' || arg.startsWith('--max-count='));
    return hasCountLimit ? commandArgs : ['--max-count', '50', ...commandArgs];
  }

  if (command === 'du') {
    const hasDepthLimit = commandArgs.some((arg) => arg === '-d' || arg.startsWith('-d') || arg.startsWith('--max-depth'));
    if (hasDepthLimit) return commandArgs;
    return process.platform === 'darwin' ? ['-d', '2', ...commandArgs] : ['--max-depth=2', ...commandArgs];
  }

  return commandArgs;
}

function rejectPathlessRead(command: string, commandArgs: string[]): string | null {
  if (!COMMANDS_WITH_LIMITED_PATHS.has(command)) return null;
  if (['pwd', 'df', 'ps', 'top'].includes(command)) return null;
  if (commandArgs.length === 0) {
    return `${command} requires at least one argument so it cannot wait for stdin.`;
  }
  return null;
}

function collectOutput(
  child: { stdout: NodeJS.ReadableStream; stderr: NodeJS.ReadableStream },
  maxBytes: number,
): { getResult(): CommandOutput } {
  let stdout: Buffer<ArrayBufferLike> = Buffer.alloc(0);
  let stderr: Buffer<ArrayBufferLike> = Buffer.alloc(0);
  let truncated = false;

  function collect(buffer: Buffer<ArrayBufferLike>, chunk: Buffer): Buffer<ArrayBufferLike> {
    const combined = Buffer.concat([buffer, chunk]);
    if (combined.length <= maxBytes) return combined;
    truncated = true;
    return combined.subarray(0, maxBytes);
  }

  child.stdout.on('data', (chunk: Buffer) => {
    stdout = collect(stdout, chunk);
  });
  child.stderr.on('data', (chunk: Buffer) => {
    stderr = collect(stderr, chunk);
  });

  return {
    getResult() {
      return {
        stdout: stdout.toString('utf8'),
        stderr: stderr.toString('utf8'),
        truncated,
      };
    },
  };
}

async function runChild(
  command: string,
  commandArgs: string[],
  cwd: string,
  timeoutMs: number,
  maxBytes: number,
): Promise<CommandResult> {
  return new Promise((resolveResult) => {
    const child = spawn(command, commandArgs, {
      cwd,
      shell: false,
      env: {
        PATH: process.env.PATH || '/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin',
        LANG: process.env.LANG || 'C.UTF-8',
      },
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    const output = collectOutput(child, maxBytes);
    let timedOut = false;

    const timer = setTimeout(() => {
      timedOut = true;
      child.kill('SIGTERM');
      setTimeout(() => {
        if (child.exitCode === null) child.kill('SIGKILL');
      }, 500);
    }, timeoutMs);

    child.on('error', (error: Error) => {
      clearTimeout(timer);
      resolveResult({
        exitCode: null,
        timedOut,
        error,
        ...output.getResult(),
      });
    });

    child.on('close', (exitCode, signal) => {
      clearTimeout(timer);
      resolveResult({
        exitCode,
        signal,
        timedOut,
        error: null,
        ...output.getResult(),
      });
    });
  });
}

export async function handleRunCommand(args: ToolArgs): Promise<string> {
  const tool = 'run_command';
  const { command, args: commandArgsRaw } = normalizeCommandInput(args);
  if (!command) {
    return errorResult(tool, 'COMMAND_REQUIRED', 'A command string is required.');
  }

  const allowed = allowedCommands();
  if (!allowed.includes(command)) {
    return errorResult(
      tool,
      'COMMAND_NOT_ALLOWED',
      `Command "${command}" is not allowed. Allowed commands: ${allowed.join(', ')}.`,
    );
  }

  const unsafeArg = rejectUnsafeArgs(commandArgsRaw);
  if (unsafeArg) {
    return errorResult(tool, 'UNSAFE_ARGUMENT', unsafeArg);
  }

  const stdinRisk = rejectPathlessRead(command, commandArgsRaw);
  if (stdinRisk) {
    return errorResult(tool, 'ARGUMENT_REQUIRED', stdinRisk);
  }

  const cwdResult = await normalizeCwd(args.cwd);
  if (!cwdResult.ok) {
    return makeResult({
      tool,
      ok: false,
      summary: cwdResult.error,
      data: { allowed_roots: cwdResult.roots },
      error: {
        code: 'CWD_NOT_ALLOWED',
        message: cwdResult.error,
      },
    });
  }

  const commandArgs = normalizeCommandArgs(command, commandArgsRaw);
  const timeoutMs = parsePositiveInt(args.timeout_ms, COMMAND_TIMEOUT_MS, 1000, COMMAND_TIMEOUT_MS);
  const maxBytes = parsePositiveInt(args.max_output_bytes, COMMAND_MAX_OUTPUT_BYTES, 1024, COMMAND_MAX_OUTPUT_BYTES);
  const startedAt = Date.now();

  logInfo('command_started', {
    command,
    args: commandArgs,
    cwd: cwdResult.cwd,
    timeoutMs,
    maxBytes,
  });

  const result = await runChild(command, commandArgs, cwdResult.cwd, timeoutMs, maxBytes);
  const durationMs = Date.now() - startedAt;

  if (result.error) {
    logError('command_failed_to_start', { command, args: commandArgs, durationMs, error: result.error });
    return errorResult(tool, 'COMMAND_START_FAILED', result.error.message);
  }

  const ok = result.exitCode === 0 && !result.timedOut;
  return makeResult({
    tool,
    ok,
    summary: result.timedOut
      ? `Command timed out after ${timeoutMs} ms.`
      : `Command exited with code ${result.exitCode}.`,
    data: {
      command,
      args: commandArgs,
      cwd: cwdResult.cwd,
      exit_code: result.exitCode,
      signal: result.signal || null,
      duration_ms: durationMs,
      stdout: result.stdout,
      stderr: result.stderr,
    },
    truncated: result.truncated,
    warnings: [
      ...(result.truncated ? [`Output exceeded ${maxBytes} bytes and was truncated.`] : []),
      ...(command === 'top' ? ['top arguments were normalized to one-shot non-interactive mode.'] : []),
    ],
    error: ok ? null : {
      code: result.timedOut ? 'COMMAND_TIMEOUT' : 'COMMAND_EXIT_NONZERO',
      message: result.timedOut ? `Command timed out after ${timeoutMs} ms.` : `Command exited with code ${result.exitCode}.`,
    },
  });
}

export async function dispatch(name: string, args: ToolArgs = {}): Promise<string> {
  const startedAt = Date.now();
  logInfo('tool_dispatch_started', { tool: name, args });

  let result;
  switch (name) {
    case 'fetch_url_content':
      result = await handleFetchUrlContent(args);
      break;
    case 'run_command':
      result = await handleRunCommand(args);
      break;
    default:
      result = errorResult(name, 'UNKNOWN_TOOL', `Unknown tool: ${name}`);
      break;
  }

  logInfo('tool_dispatch_finished', {
    tool: name,
    durationMs: Date.now() - startedAt,
  });
  return result;
}
