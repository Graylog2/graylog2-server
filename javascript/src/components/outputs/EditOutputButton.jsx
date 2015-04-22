'use strict';

var React = require('react/addons');
var OutputsStore = require('../../stores/outputs/OutputsStore');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');

var EditOutputButton = React.createClass({
    getInitialState() {
        return {
            typeDefinition: undefined,
            typeName: undefined,
            output: this.props.output,
            configurationForm: ""
        };
    },
    handleClick() {
        OutputsStore.loadAvailable(this.state.output.type, (definition) => {
            this.setState({typeDefinition: definition.requested_configuration});
            this.refs.configurationForm.open();
        });
    },
    handleSubmit(data) {
        OutputsStore.update(this.state.output, data, () => {
            this.props.onUpdate();
        });
    },
    render() {
        var typeDefinition = this.state.typeDefinition;
        var output = this.state.output;
        var configurationForm = (typeDefinition ?
            <ConfigurationForm ref="configurationForm" key={"configuration-form-output-"+output.id} configFields={this.state.typeDefinition}
                               title={"Editing Output " + output.title}
                               typeName={output.type}
                               helpBlock={<p className="help-block">{"Select a name of your new output that describes it."}</p>}
                               submitAction={this.handleSubmit} values={output.configuration} titleValue={output.title}/>
            : ""
        );
        return (
            <div>
                <button className="btn btn-success btn-xs" onClick={this.handleClick.bind(null, output)}>
                    <i className="fa fa-edit"></i> Edit
                </button>
                {configurationForm}
            </div>
        );
    }
});
module.exports = EditOutputButton;