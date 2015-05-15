'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');
var $ = require('jquery'); // excluded and shimed

var CreateAlarmCallbackButton = React.createClass({
    PLACEHOLDER: "placeholder",
    getInitialState() {
        return {
            typeName: this.PLACEHOLDER,
            typeDefinition: {}
        };
    },
    render() {
        var alarmCallbackTypes = $.map(this.props.types, this._formatOutputType);
        var humanTypeName = (this.state.typeName && this.props.types[this.state.typeName] ? this.props.types[this.state.typeName].name : "Alarm Callback");
        var configurationForm = (this.state.typeName !== this.PLACEHOLDER ? <ConfigurationForm ref="configurationForm"
                  key="configuration-form-output" configFields={this.state.typeDefinition} title={"Create new " + humanTypeName}
                  typeName={this.state.typeName} includeTitleField={false}
                  submitAction={this._handleSubmit} cancelAction={this._handleCancel} /> : null);


        return (
            <div className="form-inline">
                <div className="form-group">
                    <select id="input-type" value={this.state.typeName} onChange={this._onTypeChange} className="form-control">
                        <option value={this.PLACEHOLDER} disabled>Select Callback Type</option>
                        {alarmCallbackTypes}
                    </select>
                    {' '}
                    <button className="btn btn-success form-control" disabled={this.state.typeName === this.PLACEHOLDER}
                            onClick={this._openModal}>Add callback</button>
                </div>
                {configurationForm}
            </div>
        );
    },
    _openModal() {
        this.refs.configurationForm.open();
    },
    _formatOutputType(typeDefinition, typeName) {
        return (<option key={typeName} value={typeName}>{typeDefinition.name}</option>);
    },
    _onTypeChange(evt) {
        var alarmCallbackType = evt.target.value;
        this.setState({typeName: alarmCallbackType});
        if (this.props.types[alarmCallbackType]) {
            this.setState({typeDefinition: this.props.types[alarmCallbackType].requested_configuration});
        } else {
            this.setState({typeDefinition: {}});
        }
    },
    _handleSubmit(data) {
        this.props.onCreate(data);
        this.setState({typeName: this.PLACEHOLDER});
    },
    _handleCancel() {
        this.setState({typeName: this.PLACEHOLDER});
    }
});

module.exports = CreateAlarmCallbackButton;
