import React from 'react';
import { Table } from 'react-bootstrap';

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
            <tr>
              <td><code>{this._buildVariableName('ip')}</code></td>
              <td>First public IP address of the machine the sidecar is running on.</td>
            </tr>
            <tr>
              <td><code>{this._buildVariableName('cpuIdle')}</code></td>
              <td>Current CPU idle value.</td>
            </tr>
            <tr>
              <td><code>{this._buildVariableName('load1')}</code></td>
              <td>Current system load.</td>
            </tr>
          </tbody>
        </Table>
      </div>
    );
  }
}

export default TemplatesHelper;
