/* eslint-disable react/no-unescaped-entities */
import React from 'react';
import { Alert } from 'react-bootstrap';

class CSVFileAdapterDocumentation extends React.Component {
  render() {
    const csvFile1 = `"ipaddr","hostname"
"127.0.0.1","localhost"
"10.0.0.1","server1"
"10.0.0.2","server2"`;

    const csvFile2 = `'ipaddr';'lladdr';'hostname'
'127.0.0.1';'e4:b2:11:d1:38:14';'localhost'
'10.0.0.1';'e4:b2:12:d1:48:28';'server1'
'10.0.0.2';'e4:b2:11:d1:58:34';'server2'`;

    return (<div>
      <p>The CSV data adapter can read key value pairs from a CSV file.</p>
      <p>Please make sure your CSV file is formatted according to your configuration settings.</p>

      <Alert style={{ marginBottom: 10 }} bsStyle="info">
        <h4 style={{ marginBottom: 10 }}>CSV file requirements:</h4>
        <ul className="no-padding">
          <li>The first line in the CSV file needs to be a list of field/column names</li>
          <li>The file uses <strong>utf-8</strong> encoding</li>
          <li>The file is readable by <strong>every</strong> Graylog server node</li>
        </ul>
      </Alert>

      <hr />

      <h3 style={{ marginBottom: 10 }}>Example 1</h3>

      <h5 style={{ marginBottom: 10 }}>Configuration</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        Separator: <code>,</code><br />
        Quote character: <code>"</code><br />
        Key column: <code>ipaddr</code><br />
        Value column: <code>hostname</code>
      </p>

      <h5 style={{ marginBottom: 10 }}>CSV File</h5>
      <pre>{csvFile1}</pre>

      <h3 style={{ marginBottom: 10 }}>Example 2</h3>

      <h5 style={{ marginBottom: 10 }}>Configuration</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        Separator: <code>;</code><br />
        Quote character: <code>'</code><br />
        Key column: <code>ipaddr</code><br />
        Value column: <code>hostname</code>
      </p>

      <h5 style={{ marginBottom: 10 }}>CSV File</h5>
      <pre>{csvFile2}</pre>
    </div>);
  }
}

export default CSVFileAdapterDocumentation;
