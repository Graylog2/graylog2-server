'use strict';

var React = require('react/addons');
var PermissionsMixin = require('../../util/PermissionsMixin');
var DeleteAlarmCallbackButton = require('./DeleteAlarmCallbackButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var UserLink = require('../users/UserLink');
var EditAlarmCallbackButton = require('./EditAlarmCallbackButton');

var AlarmCallback = React.createClass({
    mixins: [PermissionsMixin],
    render() {
        var alarmCallback = this.props.alarmCallback;
        var humanReadableType = this.props.types[alarmCallback.type].name;
        var editAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["STREAMS_EDIT"]) ?
            <EditAlarmCallbackButton alarmCallback={alarmCallback} types={this.props.types} streamId={this.props.streamId} onUpdate={this.props.updateAlarmCallback} /> : "");
        var deleteAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["STREAMS_EDIT"]) ?
            <DeleteAlarmCallbackButton alarmCallback={alarmCallback} onClick={this.props.deleteAlarmCallback} /> : "");
        return (
            <div className="node-row alert-condition alert-callback" data-destination-id={alarmCallback.id}>
                <span className="pull-right node-row-info">
                    Created by <UserLink username={alarmCallback.creator_user_id} />
                    {' '}
                    <span title={moment(alarmCallback.created_at).format()}>{moment(alarmCallback.created_at).fromNow()}</span>
                    {' '}
                    {editAlarmCallbackButton}
                    {' '}
                    {deleteAlarmCallbackButton}
                </span>

                <h3>
                    <i className="fa fa-ellipsis-v"></i>
                    {' '}
                    <span>{humanReadableType}</span>
                </h3>
                <br/>

                <ConfigurationWell configuration={alarmCallback.configuration}/>
            </div>
        );
    }
});

module.exports = AlarmCallback;
