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
      <code>title:LDAP</code><br />
    </p>
  </Popover>
);

const BackendQueryHelper = () => (
  <OverlayTrigger overlay={backendQueryHelperPopover}
                  placement="right"
                  rootClose
                  trigger="click">
    <Button bsStyle="link">
      <Icon name="question-circle" />
    </Button>
  </OverlayTrigger>
);

export default BackendQueryHelper;
