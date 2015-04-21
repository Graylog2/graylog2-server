'use strict';

var React = require('react/addons');
var AlarmCallbacksStore = require('../../stores/alarmcallbacks/AlarmCallbackStore');
var AlarmCallback = require('./AlarmCallback');

var AlarmCallbackList = React.createClass({
    getInitialState() {
        return {
            permissions: this.props.permissions,
            alarmCallbacks: [],
            streamId: this.props.streamId,
            availableAlarmCallbacks: undefined
        };
    },
    loadData() {
        AlarmCallbacksStore.loadForStream(this.state.streamId, (alarmCallbacks) => {
            this.setState({alarmCallbacks: alarmCallbacks});
        });
        AlarmCallbacksStore.available(this.state.streamId, (available) => {
           this.setState({availableAlarmCallbacks: available});
        });
    },
    componentDidMount() {
        this.loadData();
    },
    _humanReadableType(alarmCallback) {
        if (this.state.availableAlarmCallbacks) {
            var available = this.state.availableAlarmCallbacks[alarmCallback.type];

            if (available) {
                return available.name;
            } else {
                return "Unknown Alarmcallback type";
            }
        }
        return <span><i className="fa fa-spin fa-spinner"></i> Loading</span>;
    },
    render() {
        var alarmCallbacks = this.state.alarmCallbacks.map((alarmCallback) => {
            return <AlarmCallback key={"alarmCallback-" + alarmCallback.id} alarmCallback={alarmCallback}
                                  humanReadableType={this._humanReadableType(alarmCallback)} permissions={this.state.permissions}/>;
        });

        return (
            <div className="alert-callbacks">
                {alarmCallbacks}
            </div>
        );
    }
});

module.exports = AlarmCallbackList;
