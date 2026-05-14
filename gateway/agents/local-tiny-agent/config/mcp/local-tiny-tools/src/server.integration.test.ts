import { createServer } from 'node:http';
import type { IncomingMessage, ServerResponse } from 'node:http';
import { rm } from 'node:fs/promises';
import { join } from 'node:path';
import type { AddressInfo } from 'node:net';
import test from 'node:test';
import assert from 'node:assert/strict';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

type TextToolContent = {
  type: string;
  text?: string;
};

type TextToolResult = {
  content: TextToolContent[];
};

type ParsedToolResult = {
  ok: boolean;
  data: Record<string, unknown>;
  error: { code: string };
};

type TestServer = {
  url: string;
  close: () => Promise<void>;
};

function parseTextResult(result: unknown): ParsedToolResult {
  assert.ok(result && typeof result === 'object' && 'content' in result);
  const textResult = result as TextToolResult;
  assert.equal(textResult.content.length, 1);
  assert.equal(textResult.content[0].type, 'text');
  return JSON.parse(textResult.content[0].text ?? '') as ParsedToolResult;
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

async function withMcpClient<T>(callback: (client: Client) => Promise<T>): Promise<T> {
  const goosePathRoot = join(process.cwd(), 'tmp-test-goose-root');
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: ['dist/index.js'],
    cwd: process.cwd(),
    env: {
      ...process.env,
      LOCAL_TINY_ALLOWED_HOSTS: 'localhost,127.0.0.1',
      LOCAL_TINY_COMMAND_ROOTS: process.cwd(),
      LOCAL_TINY_COMMAND_TIMEOUT_MS: '5000',
      LOCAL_TINY_MAX_OUTPUT_BYTES: '65536',
      LOCAL_TINY_FETCH_TIMEOUT_MS: '8000',
      LOCAL_TINY_FETCH_MAX_BYTES: '65536',
      GOOSE_PATH_ROOT: goosePathRoot,
    },
    stderr: 'pipe',
  });
  const client = new Client(
    { name: 'local-tiny-tools-test-client', version: '1.0.0' },
    { capabilities: {} },
  );

  try {
    await client.connect(transport);
    return await callback(client);
  } finally {
    await client.close();
    await rm(goosePathRoot, { recursive: true, force: true });
  }
}

test('stdio server lists and runs both tools end to end', async () => {
  const server = await withServer((req, res) => {
    res.setHeader('content-type', 'text/plain');
    res.end('mcp integration ok');
  });

  try {
    await withMcpClient(async (client) => {
      const tools = await client.listTools();
      assert.deepEqual(tools.tools.map((tool) => tool.name), [
        'fetch_url_content',
        'run_command',
      ]);

      const commandResult = parseTextResult(await client.callTool({
        name: 'run_command',
        arguments: {
          command: 'pwd',
          cwd: process.cwd(),
        },
      }));
      assert.equal(commandResult.ok, true);
      assert.equal(commandResult.data.command, 'pwd');

      const fetchResult = parseTextResult(await client.callTool({
        name: 'fetch_url_content',
        arguments: {
          url: server.url,
        },
      }));
      assert.equal(fetchResult.ok, true);
      assert.equal(fetchResult.data.body, 'mcp integration ok');
    });
  } finally {
    await server.close();
  }
});

test('stdio server returns structured errors without crashing', async () => {
  await withMcpClient(async (client) => {
    const result = parseTextResult(await client.callTool({
      name: 'run_command',
      arguments: {
        command: 'rm -rf tmp',
        cwd: process.cwd(),
      },
    }));

    assert.equal(result.ok, false);
    assert.equal(result.error.code, 'COMMAND_NOT_ALLOWED');
  });
});
