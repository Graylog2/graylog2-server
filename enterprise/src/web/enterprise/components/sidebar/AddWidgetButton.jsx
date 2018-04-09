import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import uuid from 'uuid/v4';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import WidgetActions from 'enterprise/actions/WidgetActions';

const AddWidgetButton = createReactClass({
  propTypes: {
    viewId: PropTypes.string.isRequired,
    queryId: PropTypes.string.isRequired,
  },

  getInitialState() {
    return {};
  },

  onCreateAggregation() {
    WidgetActions.create(this.props.viewId, this.props.queryId, {
      id: uuid(),
      type: 'AGGREGATION',
      config: {
        series: ['count()'],
        rowPivots: ['timestamp'],
        columnPivots: [],
        sort: [],
      },
    });
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
        <DropdownButton title="Add Widget">
          <MenuItem onSelect={this.onCreateAggregation}>Aggregation</MenuItem>
          <MenuItem onSelect={this.onCreateAlertStatus}>Alert Status</MenuItem>
          <MenuItem onSelect={this.onCreateMessageTable}>Message Table</MenuItem>
        </DropdownButton>
      </div>
    );
  },
});

export default AddWidgetButton;
