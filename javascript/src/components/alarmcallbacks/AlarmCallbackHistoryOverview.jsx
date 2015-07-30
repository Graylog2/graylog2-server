'use strict';

var React = require('react/addons');
var AlarmCallbackHistoryStore = require('../../stores/alarmcallbacks/AlarmCallbackHistoryStore');
var AlarmCallbacksStore = require('../../stores/alarmcallbacks/AlarmCallbacksStore');
var Spinner = require('../common/Spinner');
var AlarmCallbackHistory = require('./AlarmCallbackHistory');
var Col = require('react-bootstrap').Col;
var Row = require('react-bootstrap').Row;

var AlarmCallbackHistoryOverview = React.createClass({
    getInitialState() {
        return {};
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        AlarmCallbackHistoryStore.listForAlert(this.props.streamId, this.props.alertId).done((result) => {
            this.setState({histories: result.histories});
        });
        AlarmCallbacksStore.available(this.props.streamId, (types) => {
            this.setState({types: types});
        });
    },
    _formatHistory(history) {
        return <AlarmCallbackHistory key={history._id} alarmCallbackHistory={history} types={this.state.types}/>;
    },
    render() {
        if (this.state.histories && this.state.types) {
            if (this.state.histories.length > 0) {
                var histories = this.state.histories.map(this._formatHistory);
                return (
                    <Row>
                        <Col md={12}>
                            {histories}
                        </Col>
                    </Row>
                );
            } else {
                return (
                    <div><i>No history available.</i></div>
                );
            }
        } else {
            return <Spinner />;
        }
    }
});

module.exports = AlarmCallbackHistoryOverview;
