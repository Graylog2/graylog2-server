import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import Promise from 'bluebird';
import { Link } from 'react-router';

import { Alert } from 'components/alerts';
import { EntityList, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const StreamAlerts = createReactClass({
  displayName: 'StreamAlerts',
  propTypes: {
    stream: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(AlertsStore), Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      loading: false,
    };
  },

  componentDidMount() {
    this._refreshData();
  },

  currentPage: 1,
  pageSize: 5,

  loadData(pageNo, limit) {
    this.setState({ loading: true });
    const promises = [
      AlertsActions.listPaginated(this.props.stream.id, (pageNo - 1) * limit, limit),
      AlertConditionsActions.list(this.props.stream.id),
      AlertConditionsActions.available(),
    ];

    Promise.all(promises).finally(() => this.setState({ loading: false }));
  },

  _refreshData() {
    this.loadData(this.currentPage, this.pageSize);
  },

  _onChangePaginatedList(page, size) {
    this.currentPage = page;
    this.pageSize = size;
    this.loadData(page, size);
  },

  _formatAlert(alert) {
    const condition = this.state.alertConditions.find(alertCondition => alertCondition.id === alert.condition_id);
    const conditionType = condition ? this.state.types[condition.type] : {};

    return (
      <Alert key={alert.id}
             alert={alert}
             alertCondition={condition}
             stream={this.props.stream}
             conditionType={conditionType} />
    );
  },

  _isLoading() {
    return !this.state.alerts || !this.state.alertConditions || !this.state.types;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        <h2>Stream alerts</h2>
        <p className="description">
          Here you can see the currently unresolved alerts for this stream. To see resolved alerts on this stream
          or alerts on other streams, visit the <Link to={Routes.ALERTS.LIST}>alerts</Link> page.
        </p>

        <PaginatedList totalItems={this.state.alerts.total}
                       pageSize={this.pageSize}
                       onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle="success"
                      noItemsText="Good news! Currently there are no unresolved alerts on this stream."
                      items={this.state.alerts.alerts.map(alert => this._formatAlert(alert))} />
        </PaginatedList>
      </div>
    );
  },
});

export default StreamAlerts;
