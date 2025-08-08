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
/* eslint-disable react/no-unescaped-entities */
import React from 'react';

import { MantineAccordion } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

const csvFile1 = `"ipaddr","hostname"
"127.0.0.1","localhost"
"10.0.0.1","server1"
"10.0.0.2","server2"`;

const csvFile2 = `'ipaddr';'lladdr';'hostname'
'127.0.0.1';'e4:b2:11:d1:38:14';'localhost'
'10.0.0.1';'e4:b2:12:d1:48:28';'server1'
'10.0.0.2';'e4:b2:11:d1:58:34';'server2'`;

const csvFile3 = `"cidr","subnet"
"192.168.100.0/24","Finance Department subnet"
"192.168.101.0/24","IT Department subnet"
"192.168.102.0/24","HR Department subnet"`;

const CSVFileAdapterDocumentation = () => {
  const productName = useProductName();

  const accordionItems = [
    {
      value: 'example_1',
      label: 'Example 1',
      content: (
        <div>
          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Separator: <code>,</code>
            <br />
            Quote character: <code>"</code>
            <br />
            Key column: <code>ipaddr</code>
            <br />
            Value column: <code>hostname</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>CSV File</h5>
          <pre>{csvFile1}</pre>
        </div>
      ),
    },
    {
      value: 'example_2',
      label: 'Example 2',
      content: (
        <div>
          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Separator: <code>;</code>
            <br />
            Quote character: <code>'</code>
            <br />
            Key column: <code>ipaddr</code>
            <br />
            Value column: <code>hostname</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>CSV File</h5>
          <pre>{csvFile2}</pre>
        </div>
      ),
    },
    {
      value: 'cidr_lookups',
      label: 'CIDR Lookups',
      content: (
        <div>
          <p style={{ marginBottom: 10, padding: 0 }}>
            If this data adapter will be used to lookup IP address keys against CIDR addresses
            <br />
            then it should be marked as a CIDR lookup. For example:
            <br />
          </p>

          <h5 style={{ marginBottom: 10 }}>Configuration</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            Separator: <code>,</code>
            <br />
            Quote character: <code>"</code>
            <br />
            Key column: <code>cidr</code>
            <br />
            Value column: <code>subnet</code>
            <br />
            CIDR lookup: <code>true</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>CSV File</h5>
          <pre>{csvFile3}</pre>

          <p>
            Given this CSV file and configuration looking up the key 192.168.101.64 would return 'IT Department subnet'.
          </p>
        </div>
      ),
    },
  ];

  return (
    <div>
      <p>The CSV data adapter can read key value pairs from a CSV file.</p>
      <p>Please make sure your CSV file is formatted according to your configuration settings.</p>

      <h4>CSV file requirements</h4>
      <p>The first line in the CSV file needs to be a list of field/column names</p>
      <p>
        The file uses <strong>utf-8</strong> encoding
      </p>
      <p>
        The file is readable by <strong>every</strong> {productName} server node
      </p>

      <hr />

      <MantineAccordion defaultValue="example_1" accordionItems={accordionItems} />
    </div>
  );
};

export default CSVFileAdapterDocumentation;
