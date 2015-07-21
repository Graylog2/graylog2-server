'use strict';

var React = require('react/addons');
var AlertsStore = require('../../stores/alerts/AlertsStore');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Pagination = require('react-bootstrap').Pagination;
var Spinner = require('../common/Spinner');
var AlertsTable = require('./AlertsTable');
var Input = require('react-bootstrap').Input;

var AlertsComponent = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    getInitialState() {
        return {
            skip: 0,
            limit: 10,
            paginatorSize: 10,
            currentPage: 1
        };
    },
    _onSelected(event, selectedEvent) {
        var pageNo = selectedEvent.eventKey;
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
        var pageSize = parseInt(e.target.value);
        var currentPage = Math.floor((this.state.currentPage-1)*this.state.limit/pageSize)+1;
        this.setState({
            limit: pageSize,
            currentPage: currentPage
        });
        this.loadData(currentPage, pageSize);
    },
    _showPerPageSelect() {
        return (
            <Input type='select' onChange={this._onChangePageSize}>
                <option value={10}>10</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
            </Input>
        );
    },
    render() {
        if (this.state.alerts) {
            var numberPages = Math.ceil(this.state.alerts.total/this.state.limit);
            return (
                <Row className="content">
                    <Col md={12}>
                        <h2>
                            Triggered alerts

                            <small>{this.state.alerts.total} alerts total</small>
                            <div style={{float: "right"}}>
                                {this._showPerPageSelect()}
                            </div>
                        </h2>
                        {' '}
                        <AlertsTable alerts={this.state.alerts.alerts} />

                        <div className="text-center">
                            <Pagination bsSize="small" items={numberPages}
                                        activePage={this.state.currentPage}
                                        onSelect={this._onSelected} prev={true} next={true}
                                        maxButtons={Math.min(this.state.paginatorSize, numberPages)}/>
                        </div>
                    </Col>
                </Row>
            );
        } else {
            return <Spinner />;
        }
    }
});

module.exports = AlertsComponent;
