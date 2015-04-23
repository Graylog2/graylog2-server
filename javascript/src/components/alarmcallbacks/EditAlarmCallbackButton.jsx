'use strict';

var React = require('react/addons');
var AlarmCallbacksStore = require('../../stores/alarmcallbacks/AlarmCallbacksStore');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');

var EditAlarmCallbackButton = React.createClass({
    getInitialState() {
        return {
            streamId: this.props.streamId,
            typeDefinition: undefined,
            typeName: undefined,
            alarmCallback: this.props.alarmCallback,
            onSubmit: this.props.onSubmit
        };
    },
    handleClick() {
        var alarmCallback = this.state.alarmCallback;
        AlarmCallbacksStore.available(this.state.streamId, (definitions) => {
            var definition = definitions[alarmCallback.type];
            if (definition) {
                this.setState({typeDefinition: definition.requested_configuration});
                this.refs.configurationForm.open();
            }
        });
    },
    handleSubmit(data) {
        AlarmCallbacksStore.update(this.state.streamId, this.state.alarmCallback.id, data, () => {
            this.props.onUpdate();
        });
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    render() {
        var typeDefinition = this.state.typeDefinition;
        var alarmCallback = this.state.alarmCallback;
        var configurationForm = (typeDefinition ?
            <ConfigurationForm ref="configurationForm" key={"configuration-form-alarm-callback-"+alarmCallback.id} configFields={this.state.typeDefinition}
                               title={"Editing Alarm Callback "}
                               typeName={alarmCallback.type} includeTitleField={false}
                               submitAction={this.handleSubmit} values={alarmCallback.configuration} />
            : ""
        );
        return (
            <span>
                <button className="btn btn-success btn-xs" onClick={this.handleClick}>
                    <i className="fa fa-edit"></i>  Edit
                </button>
                {configurationForm}
            </span>
        );
    }
});

module.exports = EditAlarmCallbackButton;
