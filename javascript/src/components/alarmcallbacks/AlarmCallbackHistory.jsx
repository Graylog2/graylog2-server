'use strict';

var React = require('react/addons');
var AlarmCallback = require('./AlarmCallback');

var AlarmCallbackHistory = React.createClass({
    getInitialState() {
        return {
            showAlarmCallbackDetails: false
        };
    },
    _toggleDetails(e) {
        e.preventDefault();
        this.setState({showAlarmCallbackDetails: !this.state.showAlarmCallbackDetails});
    },
    _greenTick() {return <i className="fa fa-check" style={{color: "green"}}></i>},
    _redCross(errorMsg) {return <i className="fa fa-close" style={{color: "red"}}> ({errorMsg})</i>},
    _formatAlarmCallbackDetails(alarmCallback) {
        return <AlarmCallback alarmCallback={alarmCallback} hideButtons={true} types={this.props.types} />;
    },
    render() {
        var history = this.props.alarmCallbackHistory;
        var result = (history.result.type === "error" ? this._redCross(history.result.error) : this._greenTick());
        var alarmCallbackDetails = (this.state.showAlarmCallbackDetails ? this._formatAlarmCallbackDetails(history.alarmcallbackconfiguration) : null);
        var detailsIcon = (this.state.showAlarmCallbackDetails ? <i className="fa fa-minus-square-o" /> : <i className="fa fa-plus-square-o" />);
        return (
            <div>
                <a href="#" onClick={this._toggleDetails}>{detailsIcon}</a> {history._id} {result}
                <br/>
                {alarmCallbackDetails}
            </div>
        );
    }
});

module.exports = AlarmCallbackHistory;
