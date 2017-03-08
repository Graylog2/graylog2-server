import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import Immutable from 'immutable';
import { Button } from 'react-bootstrap';

import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';

import StoreProvider from 'injection/StoreProvider';
const FieldStatisticsStore = StoreProvider.getStore('FieldStatistics');
const RefreshStore = StoreProvider.getStore('Refresh');

import NumberUtils from 'util/NumberUtils';
import UserNotification from 'util/UserNotification';

const FieldStatistics = React.createClass({
  propTypes: {
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    query: React.PropTypes.string.isRequired,
    rangeType: React.PropTypes.string.isRequired,
    rangeParams: React.PropTypes.object.isRequired,
    stream: PropTypes.object,
    forceFetch: React.PropTypes.bool,
  },
  mixins: [Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer')],

  getInitialState() {
    return {
      statsLoadPending: Immutable.Map(),
      fieldStatistics: Immutable.Map(),
      sortBy: 'field',
      sortDescending: false,
    };
  },

  componentWillReceiveProps(nextProps) {
    // Reload values when executed search changes
    if (this.props.query !== nextProps.query ||
        this.props.rangeType !== nextProps.rangeType ||
        JSON.stringify(this.props.rangeParams) !== JSON.stringify(nextProps.rangeParams) ||
        this.props.stream !== nextProps.stream ||
        nextProps.forceFetch) {
      this._reloadAllStatistics();
    }
  },

  WIDGET_TYPE: 'STATS_COUNT',

  _setupTimer(refresh) {
    this._stopTimer();
    if (refresh.enabled) {
      this.timer = setInterval(this._reloadAllStatistics, refresh.interval);
    }
  },

  _stopTimer() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },

  addField(field) {
    this._reloadFieldStatistics(field);
  },

  _reloadAllStatistics() {
    this.state.fieldStatistics.keySeq().forEach(field => this._reloadFieldStatistics(field));
  },

  _reloadFieldStatistics(field) {
    if (this.isMounted) {
      this.setState({ statsLoadPending: this.state.statsLoadPending.set(field, true) });
      const promise = FieldStatisticsStore.getFieldStatistics(field);
      promise.then((statistics) => {
        this.setState({
          fieldStatistics: this.state.fieldStatistics.set(field, statistics),
          statsLoadPending: this.state.statsLoadPending.set(field, false),
        });
      }).catch((error) => {
        // if the field has no statistics to display, remove it from the set of fields (which will cause the component to not render)
        if (error.additional && error.additional.status === 400) {
          this.setState({
            fieldStatistics: this.state.fieldStatistics.delete(field),
            statsLoadPending: this.state.statsLoadPending.delete(field),
          });
        } else {
          UserNotification.error(`Loading field statistics failed with status: ${error}`,
            'Could not load field statistics');
        }
      });
    }
  },
  _changeSortOrder(column) {
    if (this.state.sortBy === column) {
      this.setState({ sortDescending: !this.state.sortDescending });
    } else {
      this.setState({ sortBy: column, sortDescending: false });
    }
  },

  _resetStatus() {
    this.setState(this.getInitialState());
  },
  _renderStatistics() {
    const statistics = [];

    this.state.fieldStatistics.keySeq()
      .sort((key1, key2) => {
        const a = this.state.sortDescending ? key2 : key1;
        const b = this.state.sortDescending ? key1 : key2;

        if (this.state.sortBy === 'field') {
          return a.toLowerCase().localeCompare(b.toLowerCase());
        }
        const statA = this.state.fieldStatistics.get(a)[this.state.sortBy];
        const statB = this.state.fieldStatistics.get(b)[this.state.sortBy];
        return NumberUtils.normalizeNumber(statA) - NumberUtils.normalizeNumber(statB);
      })
      .forEach((field) => {
        const stats = this.state.fieldStatistics.get(field);
        let maybeSpinner = null;
        if (this.state.statsLoadPending.get(field)) {
          maybeSpinner = <i className="fa fa-spin fa-spinner" />;
        }
        statistics.push(
          <tr key={field}>
            <td>{maybeSpinner}</td>
            <td>{field}</td>
            {FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
              const formatNumber = NumberUtils.isNumber(stats[statFunction]) ? NumberUtils.formatNumber(stats[statFunction]) : stats[statFunction];
              const numberStyle = {};
              if (formatNumber === 'NaN' || formatNumber === '-Infinity' || formatNumber === 'Infinity' || formatNumber === 'N/A') {
                numberStyle.color = 'lightgray';
              }
              return <td key={`${statFunction}-td`}><span style={numberStyle}>{formatNumber}</span></td>;
            })}
          </tr>,
        );
      });

    return statistics;
  },
  _renderStatisticalFunctionsHeaders() {
    return FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
      return (
        <th key={`${statFunction}-th`} onClick={() => this._changeSortOrder(statFunction)}>
          {FieldStatisticsStore.FUNCTIONS.get(statFunction)} {this._getHeaderCaret(statFunction)}
        </th>
      );
    });
  },
  _getHeaderCaret(column) {
    if (this.state.sortBy !== column) {
      return null;
    }
    return this.state.sortDescending ? <i className="fa fa-caret-down" /> : <i className="fa fa-caret-up" />;
  },
  render() {
    let content;

    if (!this.state.fieldStatistics.isEmpty()) {
      content = (
        <div className="content-col">
          <div className="pull-right">
            <AddToDashboardMenu title="Add to dashboard"
                                widgetType={this.WIDGET_TYPE}
                                bsStyle="default"
                                fields={this.state.fieldStatistics.keySeq().toJS()}
                                pullRight
                                permissions={this.props.permissions}>

              <Button bsSize="small" onClick={() => this._resetStatus()}>Dismiss</Button>
            </AddToDashboardMenu>
          </div>
          <h1>Field Statistics</h1>

          <div className="table-responsive">
            <table className="table table-striped table-bordered table-hover table-condensed">
              <thead>
                <tr>
                  <th style={{ width: 24 }} />
                  <th onClick={() => this._changeSortOrder('field')}>
                  Field {this._getHeaderCaret('field')}
                  </th>
                  {this._renderStatisticalFunctionsHeaders()}
                </tr>
              </thead>
              <tbody>
                {this._renderStatistics()}
              </tbody>
            </table>
          </div>
        </div>
      );
    } else if (!this.state.statsLoadPending.isEmpty()) {
      content = (<div className="content-col">
        <h1>Field Statistics <i className="fa fa-spin fa-spinner" /></h1>
      </div>);
    }

    return (
      <div id="field-statistics">
        {content}
      </div>
    );
  },
});

export default FieldStatistics;
