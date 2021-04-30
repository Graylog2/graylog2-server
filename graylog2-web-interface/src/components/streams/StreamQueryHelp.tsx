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

const streamQueryHelpPopover = (
  <Popover id="team-search-query-help" title="Search Syntax Help">
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
          <td>id</td>
          <td>Stream ID</td>
        </tr>
        <tr>
          <td>title</td>
          <td>Title of the stream</td>
        </tr>
        <tr>
          <td>description</td>
          <td>Description of the stream</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find streams with a description containing security:<br />
      <code>description:security</code><br />
    </p>
    <p>
      Find a stream with the id 5f4dfb9c69be46153b9a9a7b:<br />
      <code>id:5f4dfb9c69be46153b9a9a7b</code><br />
    </p>
  </Popover>
);

const StreamQueryHelp = () => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={streamQueryHelpPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

export default StreamQueryHelp;
