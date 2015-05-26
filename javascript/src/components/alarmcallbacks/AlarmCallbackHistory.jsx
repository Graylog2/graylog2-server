'use strict';

var React = require('react/addons');

var AlarmCallbackHistory = React.createClass({
    _greenTick() {return <i className="fa fa-check" style={{color: "green"}}></i>},
    _redCross(errorMsg) {return <i className="fa fa-close" style={{color: "red"}}> ({errorMsg})</i>},
    render() {
        var history = this.props.alarmCallbackHistory;
        var result = (history.result.type === "error" ? this._redCross(history.result.error) : this._greenTick());
        return <div>{history._id} {result}</div>;
    }
});

module.exports = AlarmCallbackHistory;
