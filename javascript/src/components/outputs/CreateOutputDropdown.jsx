'use strict';

var React = require('react/addons');
var OutputsStore = require('../../stores/outputs/OutputsStore');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');
var $ = require('jquery'); // excluded and shimed

var CreateOutputDropdown = React.createClass({
    getInitialState() {
        return {
            types: [],
            configurationForm: "",
            formId: "create-output-form"
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        OutputsStore.loadAvailableTypes((types) => {
            this.setState({types:types});
        });
    },
    render() {
        var outputTypes = $.map(this.state.types, this._formatOutputType);
        return (
            <div className="form-inline">
                <div className="form-group">
                    <select id="input-type" defaultValue="placeholder" onChange={this.showConfigurationForm} className="form-control">
                        <option value="placeholder" disabled>--- Select Output Type ---</option>
                        {outputTypes}
                    </select>

                    <button className="btn btn-success btn-sm" data-toggle="modal" data-target={"#" + this.state.formId}>Launch new output</button>
                    {this.state.configurationForm}
                </div>
            </div>
        );
    },
    _formatOutputType(title, typeName) {
        return (<option key={typeName} value={typeName}>{title}</option>);
    },
    showConfigurationForm(evt) {
        var outputType = evt.target.value;
        OutputsStore.loadAvailable(outputType, (definition) => {
            this.setState({configurationForm: this.formatConfigurationForm(outputType, definition.requested_configuration)});
        });
    },
    formatConfigurationForm(typeName, configuration) {
        var title = "Create new output";
        var elementName = "output";
        var formTarget = "http://google.de";
        var formId = this.state.formId;
        return (
            <ConfigurationForm configFields={configuration} title={title} typeName={typeName} elementName={elementName} formTarget={formTarget} formId={formId}/>
        );
    }
});

module.exports = CreateOutputDropdown;