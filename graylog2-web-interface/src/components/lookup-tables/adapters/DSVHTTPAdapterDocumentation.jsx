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

import { Alert } from 'components/graylog';

const DSVHTTPAdapterDocumentation = () => {
  const csvFile1 = `"127.0.0.1","localhost"
"10.0.0.1","server1"
"10.0.0.2","server2"`;

  const csvFile2 = `'127.0.0.1';'e4:b2:11:d1:38:14'
'10.0.0.1';'e4:b2:12:d1:48:28'
'10.0.0.2';'e4:b2:11:d1:58:34'`;

  return (
    <div>
      <p>The DSV data adapter can read key value pairs (or check for the presence of a key) from a DSV file.</p>
      <p>Please make sure your DSV file is formatted according to your configuration settings.</p>

      <Alert style={{ marginBottom: 10 }} bsStyle="info">
        <h4 style={{ marginBottom: 10 }}>CSV file requirements:</h4>
        <ul className="no-padding">
          <li>The file uses <strong>utf-8</strong> encoding</li>
          <li>The file is accessible using the same URL by <strong>every</strong> Graylog server node</li>
        </ul>
      </Alert>

      <hr />

      <h3 style={{ marginBottom: 10 }}>Example 1</h3>

      <h5 style={{ marginBottom: 10 }}>Configuration</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        Separator: <code>,</code><br />
        Quote character: <code>"</code><br />
      </p>

      <h5 style={{ marginBottom: 10 }}>DSV File</h5>
      <pre>{csvFile1}</pre>

      <h3 style={{ marginBottom: 10 }}>Example 2</h3>

      <h5 style={{ marginBottom: 10 }}>Configuration</h5>
      <p style={{ marginBottom: 10, padding: 0 }}>
        Separator: <code>;</code><br />
        Quote character: <code>'</code><br />
      </p>

      <h5 style={{ marginBottom: 10 }}>DSV File</h5>
      <pre>{csvFile2}</pre>
    </div>
  );
};

export default DSVHTTPAdapterDocumentation;
