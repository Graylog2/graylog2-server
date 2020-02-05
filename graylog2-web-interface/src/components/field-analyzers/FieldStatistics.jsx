import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import Immutable from 'immutable';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';

import StoreProvider from 'injection/StoreProvider';

import NumberUtils from 'util/NumberUtils';
import UserNotification from 'util/UserNotification';

const FieldStatisticsStore = StoreProvider.getStore('FieldStatistics');
const RefreshStore = StoreProvider.getStore('Refresh');

const FieldStatisticsWrap = styled.div`
  table {
    margin-top: 10px;
  }

  th {
    cursor: pointer;
  }
`;

const FieldStatistics = createReactClass({
  displayName: 'FieldStatistics',

  propTypes: {
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    query: PropTypes.string.isRequired,
    rangeType: PropTypes.string.isRequired,
    rangeParams: PropTypes.object.isRequired,
    stream: PropTypes.object,
    forceFetch: PropTypes.bool,
  },

  mixins: [Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer')],

  getDefaultProps() {
    return {
      stream: undefined,
      forceFetch: false,
    };
  },

  getInitialState() {
    return {
      statsLoadPending: Immutable.Map(),
      fieldStatistics: Immutable.Map(),
      sortBy: 'field',
      sortDescending: false,
    };
  },

  componentWillReceiveProps(nextProps) {
    const { query, rangeType, rangeParams, stream } = this.props;
    // Reload values when executed search changes
    if (query !== nextProps.query
        || rangeType !== nextProps.rangeType
        || JSON.stringify(rangeParams) !== JSON.stringify(nextProps.rangeParams)
        || stream !== nextProps.stream
        || nextProps.forceFetch) {
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
    const { fieldStatistics } = this.state;

    fieldStatistics.keySeq().forEach(field => this._reloadFieldStatistics(field));
  },

  _reloadFieldStatistics(field) {
    const { statsLoadPending, fieldStatistics } = this.state;

    if (this.isMounted) {
      this.setState({ statsLoadPending: statsLoadPending.set(field, true) });
      const promise = FieldStatisticsStore.getFieldStatistics(field);
      promise.then(
        (statistics) => {
          this.setState({
            fieldStatistics: fieldStatistics.set(field, statistics),
            statsLoadPending: statsLoadPending.delete(field),
          });
        },
        (error) => {
          // If the field has no statistics to display, remove it from the set of fields
          if (error.additional && error.additional.status === 400) {
            this.setState({ fieldStatistics: fieldStatistics.delete(field) });
          } else {
            UserNotification.error(`Loading field statistics failed with status: ${error}`,
              'Could not load field statistics');
          }
          // Reset loading state for the field after failure
          this.setState({ statsLoadPending: statsLoadPending.delete(field) });
        },
      );
    }
  },

  _changeSortOrder(column) {
    const { sortBy, sortDescending } = this.state;

    if (sortBy === column) {
      this.setState({ sortDescending: !sortDescending });
    } else {
      this.setState({ sortBy: column, sortDescending: false });
    }
  },

  _resetStatus() {
    this.setState(this.getInitialState());
  },

  _renderStatistics() {
    const { fieldStatistics, sortBy, sortDescending, statsLoadPending } = this.state;

    const statistics = [];

    fieldStatistics.keySeq()
      .sort((key1, key2) => {
        const a = sortDescending ? key2 : key1;
        const b = sortDescending ? key1 : key2;

        if (sortBy === 'field') {
          return a.toLowerCase().localeCompare(b.toLowerCase());
        }
        const statA = fieldStatistics.get(a)[sortBy];
        const statB = fieldStatistics.get(b)[sortBy];
        return NumberUtils.normalizeNumber(statA) - NumberUtils.normalizeNumber(statB);
      })
      .forEach((field) => {
        const stats = fieldStatistics.get(field);
        let maybeSpinner = null;
        if (statsLoadPending.get(field)) {
          maybeSpinner = <Icon name="spinner" spin />;
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
    const { sortBy, sortDescending } = this.state;

    if (sortBy !== column) {
      return null;
    }
    return sortDescending ? <Icon name="caret-down" /> : <Icon name="caret-up" />;
  },

  render() {
    const { fieldStatistics, statsLoadPending } = this.state;
    const { permissions } = this.props;
    let content;

    if (!fieldStatistics.isEmpty()) {
      content = (
        <div className="content-col">
          <div className="pull-right">
            <AddToDashboardMenu title="Add to dashboard"
                                widgetType={this.WIDGET_TYPE}
                                fields={fieldStatistics.keySeq().toJS()}
                                pullRight
                                permissions={permissions}>
              <Button bsSize="small" onClick={() => this._resetStatus()}><Icon name="close" /></Button>
            </AddToDashboardMenu>
          </div>
          <h1>
            Field Statistics{' '}
            {!statsLoadPending.isEmpty() && <Icon name="spinner" spin />}
          </h1>

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
    } else if (!statsLoadPending.isEmpty()) {
      content = (
        <div className="content-col">
          <h1>Field Statistics <Icon name="spinner" spin /></h1>
        </div>
      );
    }

    return (
      <FieldStatisticsWrap>
        {content}
      </FieldStatisticsWrap>
    );
  },
});

export default FieldStatistics;
