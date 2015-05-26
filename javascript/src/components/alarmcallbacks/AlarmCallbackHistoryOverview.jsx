'use strict';

var React = require('react/addons');
var AlarmCallbackHistoryStore = require('../../stores/alarmcallbacks/AlarmCallbackHistoryStore');
var Spinner = require('../common/Spinner');
var AlarmCallbackHistory = require('./AlarmCallbackHistory');

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
    },
    _formatHistory(history) {
        return <li><AlarmCallbackHistory alarmCallbackHistory={history} /></li>;
    },
    render() {
        if (this.state.histories) {
            var histories = this.state.histories.map(this._formatHistory);
            return (
                <div>
                    <h4>Alarmcallback History</h4>
                    <ul>
                        {histories}
                    </ul>
                </div>
            );
        } else {
            return <Spinner />
        }
    }
});

module.exports = AlarmCallbackHistoryOverview;
