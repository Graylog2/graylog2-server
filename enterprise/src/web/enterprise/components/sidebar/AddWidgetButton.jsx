import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import uuid from 'uuid/v4';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import WidgetActions from 'enterprise/actions/WidgetActions';
import AggregateActionHandler from '../../logic/fieldactions/AggregateActionHandler';

const AddWidgetButton = createReactClass({
  propTypes: {
    viewId: PropTypes.string.isRequired,
    queryId: PropTypes.string.isRequired,
  },

  getInitialState() {
    return {};
  },

  onCreateAggregation() {
    AggregateActionHandler(this.props.viewId, this.props.queryId, 'timestamp');
  },

  onCreateAlertStatus() {
    WidgetActions.create(this.props.viewId, this.props.queryId, {
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
    WidgetActions.create(this.props.viewId, this.props.queryId, {
      id: uuid(),
      title: 'Messages',
      type: 'MESSAGES',
      config: {
        fields: [],
        showMessageRow: true,
      },
    });
  },

  render() {
    return (
      <div>
        <DropdownButton title="Add Widget" id="add-widget-button-dropdown">
          <MenuItem onSelect={this.onCreateAggregation}>Aggregation</MenuItem>
          <MenuItem onSelect={this.onCreateAlertStatus}>Alert Status</MenuItem>
          <MenuItem onSelect={this.onCreateMessageTable}>Message Table</MenuItem>
        </DropdownButton>
      </div>
    );
  },
});

export default AddWidgetButton;
