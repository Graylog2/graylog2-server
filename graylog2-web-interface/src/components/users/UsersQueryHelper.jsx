// @flow strict
import * as React from 'react';

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

const userQueryHelperPopover = (
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
          <td>full_name</td>
          <td>The full name of a user</td>
        </tr>
        <tr>
          <td>username</td>
          <td>The users login username.</td>
        </tr>
        <tr>
          <td>email</td>
          <td>The users email.</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find users with a email containing example.com:<br />
      <kbd>email:example.com</kbd><br />
    </p>
  </Popover>
);

const UserQueryHelper = () => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={userQueryHelperPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

export default UserQueryHelper;
