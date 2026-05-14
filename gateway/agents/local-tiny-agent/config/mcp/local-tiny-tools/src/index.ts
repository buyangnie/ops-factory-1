import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { dispatch, tools } from './handlers.js';
import { LOG_FILE_PATH, logError, logInfo } from './logger.js';

type ToolArgs = Record<string, unknown>;

const server = new Server(
  { name: 'local-tiny-tools', version: '1.0.0' },
  { capabilities: { tools: {} } },
);

server.setRequestHandler(ListToolsRequestSchema, async () => {
  logInfo('list_tools_requested', { toolCount: tools.length });
  return { tools };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    const result = await dispatch(name, (args ?? {}) as ToolArgs);
    return { content: [{ type: 'text', text: result }] };
  } catch (error) {
    logError('call_tool_failed', {
      tool: name,
      args: args ?? {},
      error,
    });
    return {
      content: [{
        type: 'text',
        text: JSON.stringify({
          ok: false,
          tool: name,
          summary: 'Tool execution failed.',
          data: null,
          truncated: false,
          warnings: [],
          error: {
            code: 'TOOL_EXECUTION_FAILED',
            message: error instanceof Error ? error.message : String(error),
          },
        }, null, 2),
      }],
    };
  }
});

const transport = new StdioServerTransport();
await server.connect(transport);

logInfo('server_started', {
  transport: 'stdio',
  pid: process.pid,
  logFile: LOG_FILE_PATH,
});
