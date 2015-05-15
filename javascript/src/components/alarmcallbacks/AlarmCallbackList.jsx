'use strict';

var React = require('react/addons');
var AlarmCallback = require('./AlarmCallback');

var AlarmCallbackList = React.createClass({
    _humanReadableType(alarmCallback) {
        if (this.props.availableAlarmCallbacks) {
            var available = this.props.availableAlarmCallbacks[alarmCallback.type];

            if (available) {
                return available.name;
            } else {
                return "Unknown callback type";
            }
        }
        return <span><i className="fa fa-spin fa-spinner"></i> Loading</span>;
    },
    render() {
        var alarmCallbacks = this.props.alarmCallbacks.map((alarmCallback) => {
            return <AlarmCallback key={"alarmCallback-" + alarmCallback.id} alarmCallback={alarmCallback} streamId={this.props.streamId}
                                  types={this.props.types} permissions={this.props.permissions}
                                  deleteAlarmCallback={this.props.onDelete} updateAlarmCallback={this.props.onUpdate} />;
        });

        if (alarmCallbacks.length > 0) {
            return (
                <div className="alert-callbacks">
                    {alarmCallbacks}
                </div>
            );
        } else {
            return (
                <div className="alert alert-info no-alarm-callbacks">
                    No configured alarm callbacks.
                </div>
            );
        }
    }
});

module.exports = AlarmCallbackList;
