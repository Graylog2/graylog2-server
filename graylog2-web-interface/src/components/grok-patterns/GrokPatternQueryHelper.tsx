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
// @flow strict
import * as React from 'react';

import { Popover, Table } from 'components/graylog';

const GrokPatternQueryHelper = () => (
  <Popover id="search-query-help" className="popover-wide" title="Search Syntax Help">
    <p><strong>Available search fields</strong></p>
    <Table condensed>
      <thead>
        <tr>
          <th>Field</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>name</td>
          <td>The grok patterns name</td>
        </tr>
        <tr>
          <td>pattern</td>
          <td>The pattern of the grok pattern</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find grok patterns containing COMMON in the pattern:<br />
      <kbd>pattern:COMMON</kbd><br />
    </p>
  </Popover>
);

export default GrokPatternQueryHelper;
