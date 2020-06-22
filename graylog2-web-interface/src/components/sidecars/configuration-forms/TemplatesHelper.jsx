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
