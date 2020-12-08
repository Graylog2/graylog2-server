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

import { Table } from 'components/graylog';

class TemplatesHelper extends React.Component {
  _buildVariableName = (name) => {
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
              <td><code>{this._buildVariableName('operatingSystem')}</code></td>
              <td>Name of the operating system the sidecar is running on, e.g. <code>&quot;Linux&quot;, &quot;Windows&quot;</code></td>
            </tr>
            <tr>
              <td><code>{this._buildVariableName('nodeName')}</code></td>
              <td>The name of the sidecar, defaults to hostname if not set.</td>
            </tr>
            <tr>
              <td><code>{this._buildVariableName('nodeId')}</code></td>
              <td>UUID of the sidecar.</td>
            </tr>
            <tr>
              <td><code>{this._buildVariableName('sidecarVersion')}</code></td>
              <td>Version string of the running sidecar.</td>
            </tr>
          </tbody>
        </Table>
      </div>
    );
  }
}

export default TemplatesHelper;
