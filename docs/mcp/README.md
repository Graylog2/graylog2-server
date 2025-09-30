# MCP proxy

In order to use remote MCP servers in Claude Desktop right now, you need to run a small proxy.
`graylog-mcp-proxy.js` is such a proxy, as we go into production we want to explore other options instead, because this
one assumes you have nodejs installed.

## Configuration

### Claude Desktop

If you have Developer mode in your Claude Desktop, there is a link to the configuration file. Open that and edit it to
contain at least this entry:

```json
{
  "mcpServers": {
    "graylog": {
      "command": "node",
      "args": ["/path/to/bin/graylog-mcp-proxy.js"],
      "env": {
        "GRAYLOG_URL": "http://localhost:9000/api/mcp",
        "GRAYLOG_AUTH": "Basic YWRtaW46YWRtaW4="
      }
    }
  }
}
```

Base64 encode your access token (format `<literal-accesstoken-itself>:token` or `username:password` if you feel brace).
The URL defaults to a local development environment for now.
