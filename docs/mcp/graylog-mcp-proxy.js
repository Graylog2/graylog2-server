#!/usr/bin/env node

const http = require('http');
const https = require('https');
const url = require('url');

// Configuration
const GRAYLOG_MCP_URL = process.env.GRAYLOG_URL || 'http://localhost:9000/api/mcp';
const AUTH_HEADER = process.env.GRAYLOG_AUTH;

if (!AUTH_HEADER) {
  console.error('GRAYLOG_AUTH environment variable is required');
  process.exit(1);
}

// Environment variables for debugging and session management:
// DEBUG_MCP_PROXY=1 - Enable debug logging
// MCP_SESSION_ID=custom-session-id - Use custom session ID
// MCP_CLIENT_ID=custom-client-id - Use custom client ID

// Parse the MCP server URL
const serverUrl = url.parse(GRAYLOG_MCP_URL);
const isHttps = serverUrl.protocol === 'https:';
const httpModule = isHttps ? https : http;

// Handle stdin/stdout communication with Claude Desktop
process.stdin.setEncoding('utf8');

let buffer = '';

// Store any headers that might come from environment or Claude Desktop context
const additionalHeaders = {};

// Generate a consistent session ID for this proxy instance
const sessionId = process.env.MCP_SESSION_ID || `claude-${process.pid}-${Date.now()}`;

// Check for common MCP/session headers in environment
additionalHeaders['Mcp-Session-Id'] = sessionId;
if (process.env.MCP_CLIENT_ID) {
  additionalHeaders['X-Client-ID'] = process.env.MCP_CLIENT_ID;
}

process.stdin.on('data', (chunk) => {
  buffer += chunk;

  // Process complete JSON-RPC messages (assuming newline-delimited)
  const lines = buffer.split('\n');
  buffer = lines.pop() || ''; // Keep incomplete line in buffer

  lines.forEach((line) => {
    if (line.trim()) {
      try {
        const message = JSON.parse(line.trim());
        forwardToGraylog(message, line.trim());
      } catch (error) {
        console.error('Invalid JSON received:', error.message);
        // Send parse error back if it was a request (has id)
        const errorResponse = {
          jsonrpc: '2.0',
          id: null, // Use null for parse errors where we can't determine the id
          error: {
            code: -32700,
            message: 'Parse error'
          }
        };
        process.stdout.write(JSON.stringify(errorResponse) + '\n');
      }
    }
  });
});

function forwardToGraylog(messageObj, messageString) {
  const isNotification = !('id' in messageObj);

  // Build headers - start with required ones
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': AUTH_HEADER,
    'Content-Length': Buffer.byteLength(messageString),
    'User-Agent': 'claude-mcp-proxy/1.0.0',
    // Add any additional headers
    ...additionalHeaders
  };

  // Generate or maintain session ID for this connection
  // Session ID is already set in additionalHeaders

  // Debug logging for the first few requests
  if (messageObj.method === 'initialize' || process.env.DEBUG_MCP_PROXY) {
    console.error(`[PROXY] ${messageObj.method || 'notification'} - Session: ${sessionId}`);
    console.error(`[PROXY] Headers:`, Object.keys(headers));
  }

  const options = {
    hostname: serverUrl.hostname,
    port: serverUrl.port || (isHttps ? 443 : 80),
    path: serverUrl.path,
    method: 'POST',
    headers: headers
  };

  const req = httpModule.request(options, (res) => {
    let responseBuffer = '';

    res.on('data', (chunk) => {
      responseBuffer += chunk;
    });

    res.on('end', () => {
      if (res.statusCode >= 200 && res.statusCode < 300) {
        // Success - forward response back to Claude Desktop
        // But only if it was a request (not a notification)
        if (!isNotification && responseBuffer.trim()) {
          // Process potentially multiple JSON responses
          const responseLines = responseBuffer.trim().split('\n');
          responseLines.forEach(line => {
            if (line.trim()) {
              process.stdout.write(line.trim() + '\n');
            }
          });
        }
      } else {
        // HTTP error
        if (!isNotification) {
          const errorResponse = {
            jsonrpc: '2.0',
            id: messageObj.id,
            error: {
              code: -32603,
              message: `HTTP ${res.statusCode}: ${res.statusMessage}`
            }
          };
          process.stdout.write(JSON.stringify(errorResponse) + '\n');
        }
      }
    });
  });

  req.on('error', (error) => {
    console.error('Error forwarding to Graylog:', error);

    // Only send error response for requests (not notifications)
    if (!isNotification) {
      const errorResponse = {
        jsonrpc: '2.0',
        id: messageObj.id,
        error: {
          code: -32603,
          message: 'Internal error: ' + error.message
        }
      };
      process.stdout.write(JSON.stringify(errorResponse) + '\n');
    }
  });

  req.write(messageString);
  req.end();
}

process.on('SIGINT', () => {
  process.exit(0);
});

process.on('SIGTERM', () => {
  process.exit(0);
});
