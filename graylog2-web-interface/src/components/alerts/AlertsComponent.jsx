import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import ActionsProvider from 'injection/ActionsProvider';
const AlertsActions = ActionsProvider.getActions('Alerts');

import StoreProvider from 'injection/StoreProvider';
const AlertsStore = StoreProvider.getStore('Alerts');

import { PaginatedList, Spinner } from 'components/common';
import AlertsTable from 'components/alerts/AlertsTable';

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
      return (
        <Row className="content">
          <Col md={12}>
            <Spinner />
          </Col>
        </Row>
      );
    }

    let triggeredAlertsText;
    if (this.state.alerts.total > 0) {
      triggeredAlertsText = <span>&nbsp;<small>{this.state.alerts.total} alerts total</small></span>;
    }

    return (
      <div>
        <h2>
          Triggered alerts
          {triggeredAlertsText}
        </h2>

        <PaginatedList totalItems={this.state.alerts.total} onChange={this._onChangePaginatedList}>
          <AlertsTable alerts={this.state.alerts.alerts} />
        </PaginatedList>
      </div>
    );
  },
});

export default AlertsComponent;
