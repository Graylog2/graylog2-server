import React from 'react';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import AddMessageCountActionHandler from 'enterprise/logic/fieldactions/AddMessageCountActionHandler';
import AddMessageTableActionHandler from 'enterprise/logic/fieldactions/AddMessageTableActionHandler';

const menuTitle = <React.Fragment><i className="fa fa-plus" />{' '}Create</React.Fragment>;

const AddWidgetButton = () => (
  <DropdownButton title={menuTitle} id="add-widget-button-dropdown" bsStyle="info" pullRight>
    <MenuItem onSelect={AddMessageCountActionHandler}>Message Count</MenuItem>
    <MenuItem onSelect={AddMessageTableActionHandler}>Message Table</MenuItem>
    <MenuItem divider />
    <MenuItem onSelect={() => AggregateActionHandler('', 'timestamp')}>Custom Aggregation</MenuItem>
  </DropdownButton>
);

export default AddWidgetButton;
