import React from 'react';
import { Row, Col, Input, Pagination } from 'react-bootstrap';
import LinkedStateMixin from 'react-addons-linked-state-mixin';

import AlertsStore from 'stores/alerts/AlertsStore';
import Spinner from 'components/common/Spinner';
import AlertsTable from 'components/alerts/AlertsTable';

const AlertsComponent = React.createClass({
  mixins: [LinkedStateMixin],
  getInitialState() {
    return {
      skip: 0,
      limit: 10,
      paginatorSize: 10,
      currentPage: 1,
    };
  },
  _onSelected(event, selectedEvent) {
    const pageNo = selectedEvent.eventKey;
    this.setState({currentPage: pageNo});
    this.loadData(pageNo, this.state.limit);
  },
  componentDidMount() {
    this.loadData(this.state.currentPage, this.state.limit);
  },
  loadData(pageNo, limit) {
    AlertsStore.list(this.props.streamId, (pageNo-1)*limit, limit).done((alerts) => {
      this.setState({alerts: alerts});
    });
  },
  _onChangePageSize(e) {
    const pageSize = parseInt(e.target.value);
    const currentPage = Math.floor((this.state.currentPage-1)*this.state.limit/pageSize)+1;
    this.setState({
      limit: pageSize,
      currentPage: currentPage,
    });
    this.loadData(currentPage, pageSize);
  },
  _showPerPageSelect() {
    return (
      <Input type="select" bsSize="small" label="Show:" onChange={this._onChangePageSize}>
        <option value={10}>10</option>
        <option value={50}>50</option>
        <option value={100}>100</option>
      </Input>
    );
  },
  render() {
    if (this.state.alerts) {
      const numberPages = Math.ceil(this.state.alerts.total/this.state.limit);
      let triggeredAlertsText;
      let pagination;
      let alertsPerPageSelector;

      if (this.state.alerts.total > 0) {
        triggeredAlertsText = <span>&nbsp; <small>{this.state.alerts.total} alerts total</small></span>;
      }

      if (numberPages > 0) {
        alertsPerPageSelector = (
          <div className="form-inline" style={{float: "right"}}>
            {this._showPerPageSelect()}
          </div>
        );

        pagination = (
          <div className="text-center">
            <Pagination bsSize="small" items={numberPages}
                        activePage={this.state.currentPage}
                        onSelect={this._onSelected}
                        prev={true} next={true} first={true} last={true}
                        maxButtons={Math.min(this.state.paginatorSize, numberPages)}/>
          </div>
        );
      }

      return (
        <Row className="content">
          <Col md={12}>
            <h2>
              Triggered alerts
              {triggeredAlertsText}
              {alertsPerPageSelector}
            </h2>

            <AlertsTable alerts={this.state.alerts.alerts} />

            {pagination}
          </Col>
        </Row>
      );
    }
    return (
      <Row className="content">
        <Col md={12}>
          <Spinner />
        </Col>
      </Row>
    );
  },
});

export default AlertsComponent;
