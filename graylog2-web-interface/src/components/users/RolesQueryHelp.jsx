// @flow strict
import * as React from 'react';

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

const rolesQueryHelpPopover = (
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
          <td>Role name</td>
        </tr>
        <tr>
          <td>description</td>
          <td>Description of the Role</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find roles with a description containing creator:<br />
      <kbd>description:creator</kbd><br />
    </p>
  </Popover>
);

const RolesQueryHelp = () => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={rolesQueryHelpPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

export default RolesQueryHelp;
