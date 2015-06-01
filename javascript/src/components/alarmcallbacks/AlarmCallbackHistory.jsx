'use strict';

var React = require('react/addons');
var AlarmCallback = require('./AlarmCallback');

var AlarmCallbackHistory = React.createClass({
    _greenTick() {return <i className="fa fa-check" style={{color: "green"}}></i>;},
    _redCross() {return <i className="fa fa-close" style={{color: "red"}}></i>;},
    render() {
        var history = this.props.alarmCallbackHistory;
        var result = (history.result.type === "error" ? this._redCross() : this._greenTick());
        var subtitle = (history.result.type === "error" ? <div style={{color: "red"}}>{history.result.error}</div> : null);
        return (
            <AlarmCallback alarmCallback={history.alarmcallbackconfiguration} hideButtons={true} types={this.props.types}
                           titleAnnotation={result} subtitle={subtitle}/>
        );
    }
});

module.exports = AlarmCallbackHistory;
