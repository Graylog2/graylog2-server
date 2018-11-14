import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import { Link } from 'react-router';

import { Alert } from 'components/alerts';
import { EntityList, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');

const StreamAlerts = createReactClass({
  displayName: 'StreamAlerts',
  propTypes: {
    stream: PropTypes.object.isRequired,
    alertConditions: PropTypes.array.isRequired,
    availableConditions: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(AlertsStore)],

  getInitialState() {
    return {
      loading: true,
    };
  },

  componentDidMount() {
    this._refreshData();
  },

  currentPage: 1,
  pageSize: 5,

  loadData(pageNo, limit) {
    this.setState({ loading: true });
    AlertsActions.listPaginated(this.props.stream.id, (pageNo - 1) * limit, limit)
      .finally(() => this.setState({ loading: false }));
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
    const condition = this.props.alertConditions.find(alertCondition => alertCondition.id === alert.condition_id);
    const conditionType = condition ? this.props.availableConditions[condition.type] : {};

    return (
      <Alert key={alert.id}
             alert={alert}
             alertCondition={condition}
             stream={this.props.stream}
             conditionType={conditionType} />
    );
  },

  render() {
    if (this.state.loading) {
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
