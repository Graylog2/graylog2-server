'use strict';

var React = require('react/addons');
var AlarmCallbackList = require('./AlarmCallbackList');
var CreateAlarmCallbackButton = require('./CreateAlarmCallbackButton');
var PermissionsMixin = require('../../util/PermissionsMixin');

var AlarmCallbackComponent = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {
            permissions: JSON.parse(this.props.permissions),
            streamId: this.props.streamId
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    handleUpdate() {
        if (this.refs.alarmCallbackList) {
            this.refs.alarmCallbackList.loadData();
        }
    },
    render() {
        var createAlarmCallbackButton = (this.isPermitted(this.state.permissions, ["STREAMS_EDIT"]) ?
                <CreateAlarmCallbackButton streamId={this.state.streamId} onUpdate={this.handleUpdate} /> : "");
        return (
            <div className="alarm-callback-component">
                <div className="col-md-12">
                    {createAlarmCallbackButton}

                    <AlarmCallbackList ref="alarmCallbackList" streamId={this.state.streamId} permissions={this.state.permissions} onUpdate={this.handleUpdate} />
                </div>
            </div>
        );
    }
});

module.exports = AlarmCallbackComponent;
