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
