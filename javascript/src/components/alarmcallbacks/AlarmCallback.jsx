'use strict';

var React = require('react/addons');
var PermissionsMixin = require('../../util/PermissionsMixin');
var DeleteAlarmCallbackButton = require('./DeleteAlarmCallbackButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var EditAlarmCallbackButton = require('./EditAlarmCallbackButton');

var AlarmCallback = React.createClass({
    mixins: [PermissionsMixin],
    render() {
        var alarmCallback = this.props.alarmCallback;
        var humanReadableType = this.props.types[alarmCallback.type].name;
        var editAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["streams:edit:"+this.props.streamId]) ?
            <EditAlarmCallbackButton alarmCallback={alarmCallback} types={this.props.types} streamId={this.props.streamId} onUpdate={this.props.updateAlarmCallback} /> : "");
        var deleteAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["streams:edit:"+this.props.streamId]) ?
            <DeleteAlarmCallbackButton alarmCallback={alarmCallback} onClick={this.props.deleteAlarmCallback} /> : "");
        return (
            <div className="alert-callback" data-destination-id={alarmCallback.id}>
                <div className="row" style={{marginBottom: 0}}>
                    <div className="col-md-9">
                        <h3>
                            {' '}
                            <span>{humanReadableType}</span>
                        </h3>

                        Executed once per triggered alert condition.
                    </div>

                    <div className="col-md-3" style={{textAlign: "right"}}>
                        {' '}
                        {editAlarmCallbackButton}
                        {' '}
                        {deleteAlarmCallbackButton}
                    </div>
                </div>

                <div className="row" style={{marginBottom: 0}}>
                    <div className="col-md-12">
                        <ConfigurationWell configuration={alarmCallback.configuration}/>
                    </div>
                </div>

                <hr />
            </div>
        );
    }
});

module.exports = AlarmCallback;
