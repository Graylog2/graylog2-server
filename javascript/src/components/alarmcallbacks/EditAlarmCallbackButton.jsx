'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');

var EditAlarmCallbackButton = React.createClass({
    getInitialState() {
        return {
            typeDefinition: undefined,
            typeName: undefined
        };
    },
    _handleClick() {
        var alarmCallback = this.props.alarmCallback;
        var definition = this.props.types[alarmCallback.type];
        if (definition) {
            this.setState({typeDefinition: definition.requested_configuration});
            this.refs.configurationForm.open();
        }
    },
    _handleSubmit(data) {
        this.props.onUpdate(this.props.alarmCallback, data);
    },
    render() {
        var typeDefinition = this.state.typeDefinition;
        var alarmCallback = this.props.alarmCallback;
        var configurationForm = (typeDefinition ?
            <ConfigurationForm ref="configurationForm" key={"configuration-form-alarm-callback-"+alarmCallback.id} configFields={this.state.typeDefinition}
                               title={"Editing Alarm Callback "}
                               typeName={alarmCallback.type} includeTitleField={false}
                               submitAction={this._handleSubmit} values={alarmCallback.configuration} />
            : ""
        );
        return (
            <span>
                <button className="btn btn-success btn-xs" onClick={this._handleClick}>
                    <i className="fa fa-edit"></i>  Edit
                </button>
                {configurationForm}
            </span>
        );
    }
});

module.exports = EditAlarmCallbackButton;
