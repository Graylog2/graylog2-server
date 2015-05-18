'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');

var EditAlarmCallbackButton = React.createClass({
    _handleClick() {
        this.refs.configurationForm.open();
    },
    _handleSubmit(data) {
        this.props.onUpdate(this.props.alarmCallback, data);
    },
    render() {
        var alarmCallback = this.props.alarmCallback;
        var definition = this.props.types[alarmCallback.type];

        return (
            <span>
                <button className="btn btn-success" onClick={this._handleClick}>
                    Edit callback
                </button>
                <ConfigurationForm ref="configurationForm" key={"configuration-form-alarm-callback-"+alarmCallback.id}
                                   configFields={definition.requested_configuration}
                                   title={"Editing Alarm Callback "}
                                   typeName={alarmCallback.type} includeTitleField={false}
                                   submitAction={this._handleSubmit} values={alarmCallback.configuration} />
            </span>
        );
    }
});

module.exports = EditAlarmCallbackButton;
