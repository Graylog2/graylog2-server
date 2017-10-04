import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import Reflux from 'reflux';

import QuickValuesVisualization from 'components/visualizations/QuickValuesVisualization';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import Spinner from 'components/common/Spinner';
import UIUtils from 'util/UIUtils';
import QuickValuesOptionsForm from './QuickValuesOptionsForm';

import CombinedProvider from 'injection/CombinedProvider';

const { FieldQuickValuesStore, FieldQuickValuesActions } = CombinedProvider.get('FieldQuickValues');
const { RefreshStore } = CombinedProvider.get('Refresh');

const FieldQuickValues = React.createClass({
  propTypes: {
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    query: PropTypes.string.isRequired,
    rangeType: PropTypes.string.isRequired,
    rangeParams: PropTypes.object.isRequired,
    stream: PropTypes.object,
    forceFetch: PropTypes.bool,
    fields: PropTypes.arrayOf(PropTypes.object),
  },
  mixins: [Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer'), Reflux.connect(FieldQuickValuesStore)],

  getDefaultProps() {
    return {
      fields: [],
    };
  },

  getInitialState() {
    return {
      field: undefined,
      data: [],
      showVizOptions: false,
      options: {
        order: 'desc',
        limit: 5,
        tableSize: 50,
        stackedFields: '',
      },
    };
  },

  componentDidMount() {
    this._loadQuickValuesData();
  },
  componentWillReceiveProps(nextProps) {
    // Reload values when executed search changes
    if (this.props.query !== nextProps.query ||
        this.props.rangeType !== nextProps.rangeType ||
        JSON.stringify(this.props.rangeParams) !== JSON.stringify(nextProps.rangeParams) ||
        this.props.stream !== nextProps.stream ||
        nextProps.forceFetch) {
      this._loadQuickValuesData();
    }
  },
  componentDidUpdate(oldProps, oldState) {
    if (this.state.field !== oldState.field) {
      const element = ReactDOM.findDOMNode(this);
      UIUtils.scrollToHint(element);
    }
  },
  componentWillUnmount() {
    this._stopTimer();
  },

  WIDGET_TYPE: 'QUICKVALUES',

  _setupTimer(refresh) {
    this._stopTimer();
    if (refresh.enabled) {
      this.timer = setInterval(this._loadQuickValuesData, refresh.interval);
    }
  },
  _stopTimer() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },
  addField(field) {
    this.setState({ field: field }, () => this._loadQuickValuesData(false));
  },
  _loadQuickValuesData() {
    if (this.state.field !== undefined) {
      FieldQuickValuesActions.get(this.state.field, this.state.options);
    }
  },
  _resetStatus() {
    this.setState(this.getInitialState());
  },

  _onVizOptionsChange(newOptions) {
    this.setState({ options: newOptions, showVizOptions: false }, () => this._loadQuickValuesData());
  },

  _onVizOptionsCancel() {
    this.setState({ showVizOptions: false });
  },

  _showVizOptions() {
    this.setState({ showVizOptions: true });
  },

  render() {
    let content;

    let inner;
    if (this.state.showVizOptions) {
      inner = (
        <div style={{ marginTop: 10 }}>
          <QuickValuesOptionsForm limit={this.state.options.limit}
                                  tableSize={this.state.options.tableSize}
                                  order={this.state.options.order}
                                  stackedFields={this.state.options.stackedFields}
                                  stackedFieldsOptions={this.props.fields}
                                  onSave={this._onVizOptionsChange}
                                  onCancel={this._onVizOptionsCancel} />
        </div>
      );
    } else if (this.state.data.length === 0) {
      inner = (
        <div style={{ maxHeight: 400, marginTop: 10 }}>
          <Spinner />;
        </div>
      );
    } else {
      const dataTableTitle = `${this.state.options.order === 'desc' ? 'Top' : 'Bottom'} ${this.state.options.limit} values`;
      inner = (
        <div style={{ maxHeight: 400, overflowY: 'auto', marginTop: 10 }}>
          <QuickValuesVisualization id={this.state.field}
                                    config={{ show_pie_chart: true, show_data_table: true }}
                                    data={this.state.data}
                                    limit={this.state.options.limit}
                                    dataTableLimit={this.state.options.tableSize}
                                    dataTableTitle={dataTableTitle}
                                    sortOrder={this.state.options.order}
                                    horizontal
                                    displayAddToSearchButton
                                    displayAnalysisInformation />
        </div>
      );
    }

    if (this.state.field !== undefined) {
      content = (
        <div className="content-col">
          <div className="pull-right">
            <AddToDashboardMenu title="Add to dashboard"
                                widgetType={this.WIDGET_TYPE}
                                configuration={{ field: this.state.field }}
                                bsStyle="default"
                                pullRight
                                permissions={this.props.permissions}>
              <DropdownButton bsSize="small"
                              className="graph-settings"
                              title="Customize"
                              id="customize-field-graph-dropdown">
                <MenuItem onSelect={this._showVizOptions}>Configuration</MenuItem>
                <MenuItem divider />
                <MenuItem onSelect={() => this._resetStatus()}>Dismiss</MenuItem>
              </DropdownButton>
            </AddToDashboardMenu>
          </div>
          <h1>Quick Values for <em>{this.state.field}</em> {this.state.loadPending && <i
            className="fa fa-spin fa-spinner" />}</h1>

          {inner}
        </div>
      );
    }
    return <div id="field-quick-values">{content}</div>;
  },
});

export default FieldQuickValues;
