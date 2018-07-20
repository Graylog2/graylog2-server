import React from 'react';
import Reflux from 'reflux';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import uuid from 'uuid/v4';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';

import AggregateActionHandler from 'enterprise/logic/fieldactions/AggregateActionHandler';
import MessagesWidget from 'enterprise/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'enterprise/logic/widgets/MessagesWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import NumberVisualization from 'enterprise/components/visualizations/number/NumberVisualization';

const AddWidgetButton = createReactClass({
  propTypes: {
    queryId: PropTypes.string.isRequired,
  },

  mixins: [
    Reflux.connect(SelectedFieldsStore, 'selectedFields'),
  ],

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

  onCreateMessageCount() {
    WidgetActions.create(AggregationWidget.builder()
      .newId()
      .config(AggregationWidgetConfig.builder()
        .series(['count()'])
        .visualization(NumberVisualization.type)
        .build())
      .build());
  },

  onCreateMessageTable() {
    WidgetActions.create(MessagesWidget.builder().newId().config(new MessagesWidgetConfig(this.state.selectedFields, true)).build());
  },

  render() {
    return (
      <div>
        <DropdownButton title="Add Widget" id="add-widget-button-dropdown" bsStyle="info" pullRight>
          <MenuItem onSelect={this.onCreateAggregation}>Aggregation</MenuItem>
          <MenuItem onSelect={this.onCreateAlertStatus}>Alert Status</MenuItem>
          <MenuItem onSelect={this.onCreateMessageCount}>Message Count</MenuItem>
          <MenuItem onSelect={this.onCreateMessageTable}>Message Table</MenuItem>
        </DropdownButton>
      </div>
    );
  },
});

export default AddWidgetButton;
