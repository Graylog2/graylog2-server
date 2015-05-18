'use strict';

var React = require('react/addons');
var AlarmCallbackList = require('./AlarmCallbackList');
var CreateAlarmCallbackButton = require('./CreateAlarmCallbackButton');
var PermissionsMixin = require('../../util/PermissionsMixin');
var AlarmCallbacksStore = require('../../stores/alarmcallbacks/AlarmCallbacksStore');

var AlarmCallbackComponent = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        AlarmCallbacksStore.loadForStream(this.props.streamId, (alarmCallbacks) => {
            this.setState({alarmCallbacks: alarmCallbacks});
        });
        AlarmCallbacksStore.available(this.props.streamId, (available) => {
            this.setState({availableAlarmCallbacks: available});
        });
    },
    _deleteAlarmCallback(alarmCallback) {
        AlarmCallbacksStore.remove(this.props.streamId, alarmCallback.id, () => {
            this.loadData();
        });
    },
    _createAlarmCallback(data) {
        AlarmCallbacksStore.save(this.props.streamId, data, (result) => {
            this.loadData();
        });
    },
    _updateAlarmCallback(alarmCallback, data) {
        AlarmCallbacksStore.update(this.props.streamId, alarmCallback.id, data, () => {
            this.loadData();
        });
    },
    render() {
        var permissions = this.props.permissions;
        if (this.state.alarmCallbacks && this.state.availableAlarmCallbacks) {
            var createAlarmCallbackButton = (this.isPermitted(permissions, ["STREAMS_EDIT"]) ?
                <CreateAlarmCallbackButton streamId={this.props.streamId} types={this.state.availableAlarmCallbacks} onCreate={this._createAlarmCallback} /> : "");
            return (
                <div className="alarm-callback-component">
                    {createAlarmCallbackButton}

                    <AlarmCallbackList alarmCallbacks={this.state.alarmCallbacks}
                                       streamId={this.props.streamId}permissions={permissions} types={this.state.availableAlarmCallbacks}
                                       onUpdate={this._updateAlarmCallback} onDelete={this._deleteAlarmCallback} onCreate={this._createAlarmCallback} />
                </div>
            );
        } else {
            return (
                <div className="alarm-callback-component">
                    <i className="fa fa-spin fa-spinner"/> Loading
                </div>
            );
        }
    }
});

module.exports = AlarmCallbackComponent;
