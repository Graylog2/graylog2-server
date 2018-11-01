import React from 'react';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import AddMessageCountActionHandler from 'enterprise/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler from 'enterprise/logic/fieldactions/AddMessageTableActionHandler';

const AddWidgetButton = () => (
  <DropdownButton title="Add Widget" id="add-widget-button-dropdown" bsStyle="info" pullRight>
    <MenuItem onSelect={() => AggregateActionHandler('', 'timestamp')}>Aggregation</MenuItem>
    <MenuItem onSelect={AddMessageCountActionHandler}>Message Count</MenuItem>
    <MenuItem onSelect={AddMessageTableActionHandler}>Message Table</MenuItem>
  </DropdownButton>
);

export default AddWidgetButton;
