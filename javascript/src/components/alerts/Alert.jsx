/* global momentHelper, $ */

'use strict';

var React = require('react/addons');
var AlarmCallbackHistoryOverview = require('../alarmcallbacks/AlarmCallbackHistoryOverview');
var HideableElement = require('../common/HideableElement');

var Alert = React.createClass({
    getInitialState() {
        return {
            showAlarmCallbackHistory: false
        };
    },
    _onClickConditionId(conditionId) {
        var alertConditionElem = $(".alert-condition[data-condition-id=" + conditionId + "]")
        $("html, body").animate({ scrollTop: 0 }, "fast");
        alertConditionElem.effect(
            "highlight", { duration: 2000 }
        );
    },
    _toggleHistory(e) {
        e.preventDefault();
        this.setState({showAlarmCallbackHistory: !this.state.showAlarmCallbackHistory});
    },
    render() {
        var alert = this.props.alert;
        var alarmCallbackHistory = (this.state.showAlarmCallbackHistory ? <div><br/><AlarmCallbackHistoryOverview alertId={alert.id} streamId={alert.stream_id} /></div>:null);
        var historyIcon = (this.state.showAlarmCallbackHistory ? <i className="fa fa-minus-square-o" /> : <i className="fa fa-plus-square-o" />);
        return (
            <tr>
                <td>
                    {momentHelper.toUserTimeZone(moment(alert.triggered_at)).fromNow()}
                </td>
                <td style={{display: 'none'}}>{momentHelper.toUserTimeZone(moment(alert.triggered_at)).format()}</td>
                <td>
                    <a href="#" onClick={this._onClickConditionId.bind(this, alert.condition_id)}>{alert.condition_id}</a>
                </td>
                <td>
                    <a href="#" onClick={this._toggleHistory}>{historyIcon}</a> {alert.description}
                    {alarmCallbackHistory}
                </td>
            </tr>
        );
    }
});

module.exports = Alert;
