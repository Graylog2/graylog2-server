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
    handleSubmit(data) {
        OutputsStore.save(data, () => {
            console.log("saved");
            this.props.onUpdate();
        });
    },
    formatConfigurationForm(typeName, configuration) {
        var title = "Create new output";
        var formId = this.state.formId;
        var helpBlock = (<p className="help-block">{"Select a name of your new output that describes it."}</p>);
        var submitAction = this.handleSubmit;
        return (
            <ConfigurationForm key="configuration-form-output" configFields={configuration} title={title} typeName={typeName}
                               formId={formId} helpBlock={helpBlock} submitAction={submitAction}/>
        );
    }
});

module.exports = CreateOutputDropdown;