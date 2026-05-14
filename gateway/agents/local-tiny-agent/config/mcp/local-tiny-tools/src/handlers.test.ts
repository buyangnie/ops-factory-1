import { createServer } from 'node:http';
import type { IncomingMessage, Server, ServerResponse } from 'node:http';
import type { AddressInfo } from 'node:net';
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  handleFetchUrlContent,
  handleRunCommand,
} from './handlers.js';

type ParsedToolResult = {
  ok: boolean;
  tool?: string;
  data: Record<string, unknown>;
  error: { code: string };
  truncated?: boolean;
};

type TestServer = {
  url: string;
  close: () => Promise<void>;
};

function parseResult(raw: string): ParsedToolResult {
  return JSON.parse(raw) as ParsedToolResult;
}

function withServer(handler: (req: IncomingMessage, res: ServerResponse) => void): Promise<TestServer> {
  const server = createServer(handler);
  return new Promise((resolve, reject) => {
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address() as AddressInfo;
      resolve({
        url: `http://127.0.0.1:${address.port}`,
        close: () => new Promise<void>((closeResolve) => {
          server.close(() => closeResolve());
        }),
      });
    });
  });
}

test('run_command accepts a simple whitelisted command string', async () => {
  const result = parseResult(await handleRunCommand({
    command: 'pwd',
    cwd: process.cwd(),
  }));

  assert.equal(result.ok, true);
  assert.equal(result.tool, 'run_command');
  assert.equal(result.data.command, 'pwd');
  assert.equal(result.data.exit_code, 0);
});

test('run_command splits inline args from command string', async () => {
  const result = parseResult(await handleRunCommand({
    command: 'ls -d .',
    cwd: process.cwd(),
  }));

  assert.equal(result.ok, true);
  assert.equal(result.data.command, 'ls');
  assert.deepEqual(result.data.args, ['-d', '.']);
  assert.equal(result.data.exit_code, 0);
});

test('run_command rejects non-whitelisted commands', async () => {
  const result = parseResult(await handleRunCommand({
    command: 'rm -rf tmp',
    cwd: process.cwd(),
  }));

  assert.equal(result.ok, false);
  assert.equal(result.error.code, 'COMMAND_NOT_ALLOWED');
});

test('run_command rejects commands that can mutate files by default', async () => {
  const sedResult = parseResult(await handleRunCommand({
    command: 'sed -i s/a/b/g file.txt',
    cwd: process.cwd(),
  }));
  const findResult = parseResult(await handleRunCommand({
    command: 'find . -delete',
    cwd: process.cwd(),
  }));

  assert.equal(sedResult.ok, false);
  assert.equal(sedResult.error.code, 'COMMAND_NOT_ALLOWED');
  assert.equal(findResult.ok, false);
  assert.equal(findResult.error.code, 'COMMAND_NOT_ALLOWED');
});

test('run_command returns structured error for missing command', async () => {
  const result = parseResult(await handleRunCommand({}));

  assert.equal(result.ok, false);
  assert.equal(result.error.code, 'COMMAND_REQUIRED');
});

test('run_command rejects shell operators in arguments', async () => {
  const result = parseResult(await handleRunCommand({
    command: 'ls',
    args: ['.', '&&', 'pwd'],
    cwd: process.cwd(),
  }));

  assert.equal(result.ok, false);
  assert.equal(result.error.code, 'UNSAFE_ARGUMENT');
});

test('run_command rejects cwd outside allowed roots', async () => {
  const previousRoots = process.env.LOCAL_TINY_COMMAND_ROOTS;
  process.env.LOCAL_TINY_COMMAND_ROOTS = process.cwd();
  try {
    const result = parseResult(await handleRunCommand({
      command: 'pwd',
      cwd: '/',
    }));

    assert.equal(result.ok, false);
    assert.equal(result.error.code, 'CWD_NOT_ALLOWED');
  } finally {
    if (previousRoots === undefined) {
      delete process.env.LOCAL_TINY_COMMAND_ROOTS;
    } else {
      process.env.LOCAL_TINY_COMMAND_ROOTS = previousRoots;
    }
  }
});

test('fetch_url_content fetches localhost text', async () => {
  const server = await withServer((req, res) => {
    res.setHeader('content-type', 'text/plain');
    res.end('hello local tiny agent');
  });

  try {
    const result = parseResult(await handleFetchUrlContent({
      url: server.url,
    }));

    assert.equal(result.ok, true);
    assert.equal(result.data.status, 200);
    assert.equal(result.data.body, 'hello local tiny agent');
  } finally {
    await server.close();
  }
});

test('fetch_url_content accepts URL without scheme', async () => {
  const server = await withServer((req, res) => {
    res.setHeader('content-type', 'application/json');
    res.end(JSON.stringify({ ok: true }));
  });

  try {
    const noSchemeUrl = server.url.replace(/^http:\/\//, '');
    const result = parseResult(await handleFetchUrlContent({
      url: noSchemeUrl,
    }));

    assert.equal(result.ok, true);
    assert.equal(result.data.status, 200);
    assert.equal(JSON.parse(String(result.data.body)).ok, true);
  } finally {
    await server.close();
  }
});

test('fetch_url_content truncates long content', async () => {
  const server = await withServer((req, res) => {
    res.setHeader('content-type', 'text/plain');
    res.end('x'.repeat(4096));
  });

  try {
    const result = parseResult(await handleFetchUrlContent({
      url: server.url,
      max_bytes: 1024,
    }));

    assert.equal(result.ok, true);
    assert.equal(result.truncated, true);
    assert.equal(String(result.data.body).length, 1024);
  } finally {
    await server.close();
  }
});

test('fetch_url_content returns structured error for missing URL', async () => {
  const result = parseResult(await handleFetchUrlContent({}));

  assert.equal(result.ok, false);
  assert.equal(result.error.code, 'URL_REQUIRED');
});

test('fetch_url_content rejects non-local hosts by default', async () => {
  const result = parseResult(await handleFetchUrlContent({
    url: 'https://example.com',
  }));

  assert.equal(result.ok, false);
  assert.equal(result.error.code, 'HOST_NOT_ALLOWED');
});

test('fetch_url_content rejects redirect to disallowed host', async () => {
  const server = await withServer((req, res) => {
    res.statusCode = 302;
    res.setHeader('location', 'https://example.com/');
    res.end();
  });

  try {
    const result = parseResult(await handleFetchUrlContent({
      url: server.url,
    }));

    assert.equal(result.ok, false);
    assert.equal(result.error.code, 'REDIRECT_HOST_NOT_ALLOWED');
  } finally {
    await server.close();
  }
});
