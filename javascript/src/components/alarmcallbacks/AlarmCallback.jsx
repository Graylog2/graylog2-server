'use strict';

var React = require('react/addons');
var PermissionsMixin = require('../../util/PermissionsMixin');
var DeleteAlarmCallbackButton = require('./DeleteAlarmCallbackButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var UserLink = require('../users/UserLink');
var EditAlarmCallbackButton = require('./EditAlarmCallbackButton');

var AlarmCallback = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {
            alarmCallback: this.props.alarmCallback,
            streamId: this.props.streamId,
            permissions: this.props.permissions,
            humanReadableType: this.props.humanReadableType,
            deleteAlarmCallback: this.props.deleteAlarmCallback,
            updateAlarmCallback: this.props.updateAlarmCallback
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    render() {
        var alarmCallback = this.state.alarmCallback;
        var editAlarmCallbackButton = (this.isPermitted(this.state.permissions, ["STREAMS_EDIT"]) ?
            <EditAlarmCallbackButton alarmCallback={alarmCallback} streamId={this.state.streamId} onUpdate={this.state.updateAlarmCallback} /> : "");
        var deleteAlarmCallbackButton = (this.isPermitted(this.state.permissions, ["STREAMS_EDIT"]) ?
            <DeleteAlarmCallbackButton alarmCallback={alarmCallback} onClick={this.state.deleteAlarmCallback} /> : "");
        return (
            <div className="row node-row alert-condition alert-callback" data-destination-id={alarmCallback.id}>
                <span className="pull-right node-row-info">
                    Created by <UserLink username={alarmCallback.creator_user_id} />
                    <span className="text" title={moment(alarmCallback.created_at).format()}>{moment(alarmCallback.created_at).fromNow()}</span>

                    {editAlarmCallbackButton}
                    {deleteAlarmCallbackButton}
                    </span>

                <h3>
                    <i className="fa fa-ellipsis-vertical"></i>
                    <span>{this.state.humanReadableType}</span>
                </h3>

                <ConfigurationWell configuration={alarmCallback.configuration}/>
            </div>
        );
    }
});

module.exports = AlarmCallback;
