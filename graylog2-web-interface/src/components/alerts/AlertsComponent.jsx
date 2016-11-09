import React from 'react';
import Reflux from 'reflux';

import ActionsProvider from 'injection/ActionsProvider';
const AlertsActions = ActionsProvider.getActions('Alerts');

import StoreProvider from 'injection/StoreProvider';
const AlertsStore = StoreProvider.getStore('Alerts');

import Alert from 'components/alerts/Alert';
import { EntityList, PaginatedList, Spinner } from 'components/common';

const AlertsComponent = React.createClass({
  mixins: [Reflux.connect(AlertsStore)],

  componentDidMount() {
    this.loadData(1, 10);
  },

  loadData(pageNo, limit) {
    AlertsActions.listAllPaginated((pageNo - 1) * limit, limit);
  },

  _onChangePaginatedList(page, size) {
    this.loadData(page, size);
  },

  render() {
    if (!this.state.alerts) {
      return <Spinner />;
    }

    return (
      <div>
        <h2>Alerts</h2>
        <p>Check your alerts status from here. Currently displaying <b>all</b> alerts.</p>

        <PaginatedList totalItems={this.state.alerts.total} onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle="info" noItemsText="There are no alerts to display."
                      items={this.state.alerts.alerts.map(alert => <Alert key={alert.id} alert={alert} />)} />
        </PaginatedList>
      </div>
    );
  },
});

export default AlertsComponent;
