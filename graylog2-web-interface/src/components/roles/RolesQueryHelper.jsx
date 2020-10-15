// @flow strict
import * as React from 'react';

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

const RolesQueryHelperPopover = (
  <Popover id="role-search-query-help" title="Search Syntax Help">
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
          <td>The name of a role</td>
        </tr>
        <tr>
          <td>description</td>
          <td>The description of a role</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find role with a name containing manager:<br />
      <kbd>name:manager</kbd><br />
    </p>
  </Popover>
);

const RolesQueryHelper = () => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={RolesQueryHelperPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

export default RolesQueryHelper;
