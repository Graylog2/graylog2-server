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
import * as React from 'react';

import { OverlayTrigger, Icon } from 'components/common';
import { Popover, Table, Button } from 'components/bootstrap';

const rolesQueryHelpPopover = (
  <Popover id="role-search-query-help"
           title="Search Syntax Help"
           data-app-section="role_search_query_helper"
           data-event-element="Available search fields">
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
