'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');
var Button = require('react-bootstrap').Button;

var EditAlarmCallbackButton = React.createClass({
    _handleClick() {
        this.refs.configurationForm.open();
    },
    _handleSubmit(data) {
        this.props.onUpdate(this.props.alarmCallback, data);
    },
    getDefaultProps() {
        return {
            disabled: false
        };
    },
    render() {
        var alarmCallback = this.props.alarmCallback;
        var definition = this.props.types[alarmCallback.type];
        var configurationForm = (definition ? <ConfigurationForm ref="configurationForm" key={"configuration-form-alarm-callback-"+alarmCallback.id}
                                                                 configFields={definition.requested_configuration}
                                                                 title={"Editing Alarm Callback "}
                                                                 typeName={alarmCallback.type} includeTitleField={false}
                                                                 submitAction={this._handleSubmit} values={alarmCallback.configuration} /> : null);

        return (
            <span>
                <Button bsStyle="success" disabled={this.props.disabled} onClick={this._handleClick}>
                    Edit callback
                </Button>
                {configurationForm}
            </span>
        );
    }
});

module.exports = EditAlarmCallbackButton;
