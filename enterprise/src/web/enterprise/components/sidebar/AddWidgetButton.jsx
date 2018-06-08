import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import uuid from 'uuid/v4';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import AggregateActionHandler from '../../logic/fieldactions/AggregateActionHandler';
import MessagesWidget from '../../logic/widgets/MessagesWidget';
import MessagesWidgetConfig from '../../logic/widgets/MessagesWidgetConfig';

const AddWidgetButton = createReactClass({
  propTypes: {
    queryId: PropTypes.string.isRequired,
  },

  getInitialState() {
    return {};
  },

  onCreateAggregation() {
    AggregateActionHandler(this.props.queryId, 'timestamp');
  },

  onCreateAlertStatus() {
    // TODO: Replace with proper object.
    WidgetActions.create({
      id: uuid(),
      title: 'Alert Status',
      type: 'ALERT_STATUS',
      config: {
        title: 'Alert Status',
        triggered: false,
        bgColor: '#8bc34a',
        triggeredBgColor: '#d32f2f',
        text: 'OK',
      },
    });
  },

  onCreateMessageTable() {
    WidgetActions.create(new MessagesWidget(uuid(), new MessagesWidgetConfig([], true)));
  },

  render() {
    return (
      <div>
        <DropdownButton title="Add Widget" id="add-widget-button-dropdown" bsStyle="info" pullRight>
          <MenuItem onSelect={this.onCreateAggregation}>Aggregation</MenuItem>
          <MenuItem onSelect={this.onCreateAlertStatus}>Alert Status</MenuItem>
          <MenuItem onSelect={this.onCreateMessageTable}>Message Table</MenuItem>
        </DropdownButton>
      </div>
    );
  },
});

export default AddWidgetButton;
