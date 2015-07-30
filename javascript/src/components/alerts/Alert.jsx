/* global momentHelper, $ */

'use strict';

var React = require('react/addons');
var Button = require('react-bootstrap').Button;
var AlarmCallbackHistoryOverview = require('../alarmcallbacks/AlarmCallbackHistoryOverview');

var Alert = React.createClass({
    getInitialState() {
        return {
            showAlarmCallbackHistory: false
        };
    },
    _onClickConditionId(conditionId) {
        var alertConditionElem = $(".alert-condition[data-condition-id=" + conditionId + "]");
        $("html, body").animate({ scrollTop: 0 }, "fast");
        alertConditionElem.effect(
            "highlight", { duration: 2000 }
        );
    },
    _toggleHistory(e) {
        e.preventDefault();
        this.setState({showAlarmCallbackHistory: !this.state.showAlarmCallbackHistory});
    },
    _getAlarmCallbackHistory(alert) {
        return (
            <tr>
                <td colSpan="2">&nbsp;</td>
                <td colSpan="2">
                    <AlarmCallbackHistoryOverview alertId={alert.id} streamId={alert.stream_id}/>
                </td>
            </tr>
        );
    },
    render() {
        var alert = this.props.alert;
        var toggleHistoryText = this.state.showAlarmCallbackHistory ? "Hide callbacks" : "Show callbacks";
        var alertTriggeredAt = momentHelper.toUserTimeZone(alert.triggered_at);
        var alarmCallbackHistory= this.state.showAlarmCallbackHistory ? this._getAlarmCallbackHistory(alert) : null;
        return (
            <tbody>
            <tr>
                <td style={{borderTop: 0}}>
                    <time title={alertTriggeredAt.format()} dateTime={alertTriggeredAt.format()}>
                        {alertTriggeredAt.fromNow()}
                    </time>
                </td>
                <td style={{borderTop: 0}}>
                    <a href="#" onClick={this._onClickConditionId.bind(this, alert.condition_id)}>{alert.condition_id}</a>
                </td>
                <td style={{borderTop: 0}}>
                    {alert.description}
                </td>
                <td className="text-right" style={{borderTop: 0}}>
                    <Button bsStyle="info" bsSize="xsmall" onClick={this._toggleHistory}>{toggleHistoryText}</Button>
                </td>
            </tr>
            {alarmCallbackHistory}
            </tbody>
        );
    }
});

module.exports = Alert;
