/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
export const DEFAULT_BODY_TEMPLATE = `--- [Event Definition] ---------------------------
Title:       \${event_definition_title}
Description: \${event_definition_description}
Type:        \${event_definition_type}
--- [Event] --------------------------------------
Alert Replay:         \${http_external_uri}alerts/\${event.id}/replay-search
Timestamp:            \${event.timestamp}
Message:              \${event.message}
Source:               \${event.source}
Key:                  \${event.key}
Priority:             \${event.priority}
Alert:                \${event.alert}
Timestamp Processing: \${event.timestamp}
Timerange Start:      \${event.timerange_start}
Timerange End:        \${event.timerange_end}
\${if event.fields}
Fields:
\${foreach event.fields field}  \${field.key}: \${field.value}
\${end}
\${end}
\${if event.group_by_fields}
Group By Fields:
\${foreach event.group_by_fields field}  \${field.key}: \${field.value}
\${end}
\${end}
\${if event.aggregation_conditions}
Aggregation Conditions:
\${foreach event.aggregation_conditions condition}  \${condition.key}: \${condition.value}
\${end}
\${end}
\${if backlog}
--- [Backlog] ------------------------------------
Last messages accounting for this alert:
\${foreach backlog message}
\${message}
\${end}
\${end}
`;

export const DEFAULT_HTML_BODY_TEMPLATE = `<table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
<tr style="line-height:1.5"><th colspan="2" style="background-color:#e6e6e6">Event Definition</th></tr>
<tr><td width="200px">Title</td><td>\${event_definition_title}</td></tr>
<tr><td>Description</td><td>\${event_definition_description}</td></tr>
<tr><td>Type</td><td>\${event_definition_type}</td></tr>
</tbody></table>
<br /><table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
<tr><th colspan="2" style="background-color:#e6e6e6;line-height:1.5">Event</th></tr>
<tr><td>Alert Replay</td><td>\${http_external_uri}alerts/\${event.id}/replay-search</td></tr>
<tr><td width="200px">Timestamp</td><td>\${event.timestamp}</td></tr>
<tr><td>Message</td><td>\${event.message}</td></tr>
<tr><td>Source</td><td>\${event.source}</td></tr>
<tr><td>Key</td><td>\${event.key}</td></tr>
<tr><td>Priority</td><td>\${event.priority}</td></tr>
<tr><td>Alert</td><td>\${event.alert}</td></tr>
<tr><td>Timestamp Processing</td><td>\${event.timestamp}</td></tr>
<tr><td>Timerange Start</td><td>\${event.timerange_start}</td></tr>
<tr><td>Timerange End</td><td>\${event.timerange_end}</td></tr>
<tr><td>Source Streams</td><td>\${event.source_streams}</td></tr>
\${if event.fields}
<tr><td>Fields</td><td><ul style="list-style-type:square;">\${foreach event.fields field}<li>\${field.key}:\${field.value}</li>\${end}<ul></td></tr>
\${end}
\${if event.group_by_fields}
<tr><td>Group By Fields</td><td><ul style="list-style-type:square;">\${foreach event.group_by_fields field}<li>\${field.key}:\${field.value}</li>\${end}<ul></td></tr>
\${end}
\${if event.aggregation_conditions}
<tr><td>Aggregation Conditions</td><td><ul style="list-style-type:square;">\${foreach event.aggregation_conditions condition}<li>\${condition.key}:\${condition.value}</li>\${end}<ul></td></tr>
\${end}
</tbody></table>
\${if backlog}
<br /><table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
<tr><th style="background-color:#e6e6e6;line-height:1.5">Backlog (Last messages accounting for this alert)</th></tr>
\${foreach backlog message}
<tr><td>\${message}</td></tr>
\${end}
</tbody></table>
\${end}
`;
