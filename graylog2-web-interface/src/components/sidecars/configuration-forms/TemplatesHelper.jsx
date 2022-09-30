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
import React from 'react';

import { Table } from 'components/bootstrap';

class TemplatesHelper extends React.Component {
  static _buildVariableName = (name) => {
    return `\${sidecar.${name}}`;
  };

  render() {
    return (
      <div>
        <Table responsive>
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>{TemplatesHelper._buildVariableName('operatingSystem')}</code></td>
              <td>Name of the operating system the sidecar is running on, e.g. <code>&quot;Linux&quot;, &quot;Windows&quot;</code></td>
            </tr>
            <tr>
              <td><code>{TemplatesHelper._buildVariableName('nodeName')}</code></td>
              <td>The name of the sidecar, defaults to hostname if not set.</td>
            </tr>
            <tr>
              <td><code>{TemplatesHelper._buildVariableName('nodeId')}</code></td>
              <td>UUID of the sidecar.</td>
            </tr>
            <tr>
              <td><code>{TemplatesHelper._buildVariableName('sidecarVersion')}</code></td>
              <td>Version string of the running sidecar.</td>
            </tr>
            <tr>
              <td><code>{TemplatesHelper._buildVariableName('spoolDir')}</code></td>
              <td>A directory that is unique per configuration and can be used to store collector data.</td>
            </tr>
            <tr>
              <td><code>{TemplatesHelper._buildVariableName('tags.<tag>')}</code></td>
              <td>A map of tags that are set for the sidecar. This can be used to render conditional configuration snippets. e.g.: <br />
                <code> &lt;#if sidecar.tags.webserver??&gt;<br />&nbsp;&nbsp;- /var/log/apache/*.log<br />&lt;/#if&gt;  </code>
              </td>
            </tr>
          </tbody>
        </Table>
      </div>
    );
  }
}

export default TemplatesHelper;
