import React from 'react';
import { Row, Col, Input, Pagination } from 'react-bootstrap';
import LinkedStateMixin from 'react-addons-linked-state-mixin';

import AlertsStore from 'stores/alerts/AlertsStore';
import { PaginatedList, Spinner } from 'components/common';
import AlertsTable from 'components/alerts/AlertsTable';

const AlertsComponent = React.createClass({
  mixins: [LinkedStateMixin],
  componentDidMount() {
    this.loadData(1, 10);
  },
  getInitialState() {
    return {};
  },
  loadData(pageNo, limit) {
    AlertsStore.list(this.props.streamId, (pageNo-1)*limit, limit).done((alerts) => {
      this.setState({alerts: alerts});
    });
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

    const triggeredAlertsText = this.state.alerts.total > 0 ? <span>&nbsp; <small>{this.state.alerts.total} alerts total</small></span> : null;

    return (
      <Row className="content">
        <Col md={12}>
          <h2>
            Triggered alerts
            {triggeredAlertsText}
          </h2>

          <PaginatedList totalItems={this.state.alerts.total} onChange={this._onChangePaginatedList}>
            <AlertsTable alerts={this.state.alerts.alerts} />
          </PaginatedList>
        </Col>
      </Row>
    );
  },
});

export default AlertsComponent;
