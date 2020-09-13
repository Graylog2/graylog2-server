// @flow strict
import * as React from 'react';

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

const backendQueryHelperPopover = (
  <Popover id="user-search-query-help" title="Search Syntax Help">
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
          <td>title</td>
          <td>The title of a backend</td>
        </tr>
        <tr>
          <td>description</td>
          <td>The description of a backends.</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find backends with a title containing LDAP:<br />
      <kbd>title:LDAP</kbd><br />
    </p>
  </Popover>
);

const BackendQueryHelper = () => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={backendQueryHelperPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

export default BackendQueryHelper;
