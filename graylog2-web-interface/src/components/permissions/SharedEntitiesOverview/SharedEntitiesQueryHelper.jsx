// @flow strict
import * as React from 'react';

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

const sharedEntitiesQueryHelperPopover = (
  <Popover id="shared-entities-search-query-help" title="Search Syntax Help">
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
          <td>Title of a shared entity.</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find shared entities with a title containing security:<br />
      <kbd>title:security</kbd><br />
    </p>
  </Popover>
);

const SharedEntitiesQueryHelper = () => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={sharedEntitiesQueryHelperPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

export default SharedEntitiesQueryHelper;
