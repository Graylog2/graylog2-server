'use strict';

var React = require('react/addons');
var AlertsStore = require('../../stores/alerts/AlertsStore');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Spinner = require('../common/Spinner');
var AlertsTable = require('./AlertsTable');
var Paginator = require('../common/Paginator');

var AlertsComponent = React.createClass({
    getInitialState() {
        return {
            skip: 0,
            limit: 10,
            currentPage: 1
        };
    },
    _onSelected(pageNo) {
        this.setState({currentPage: pageNo});
        this.loadData(pageNo);
    },
    componentDidMount() {
        this.loadData(this.state.currentPage);
    },
    loadData(pageNo) {
        AlertsStore.list(this.props.streamId, (pageNo-1)*this.state.limit, this.state.limit).done((alerts) => {
            this.setState({alerts: alerts});
        });
    },
    render() {
        if (this.state.alerts) {
            return (
                <Row className="content">
                    <Col md={12}>
                        <h2>
                            Triggered alerts

                            <small>{this.state.alerts.total} alerts total</small>
                        </h2>
                        {' '}

                        <AlertsTable alerts={this.state.alerts.alerts} />
                        <Paginator pages={Math.ceil(this.state.alerts.total/this.state.limit)} size={this.state.limit}
                                   currentPage={this.state.currentPage} onSelected={this._onSelected}/>
                    </Col>
                </Row>
            );
        } else {
            return <Spinner />;
        }
    }
});

module.exports = AlertsComponent;
