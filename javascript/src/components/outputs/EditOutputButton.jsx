'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');
var Button = require('react-bootstrap').Button;

var EditOutputButton = React.createClass({
    getInitialState() {
        return {
            typeDefinition: undefined,
            typeName: undefined,
            configurationForm: ""
        };
    },
    handleClick() {
        this.props.getTypeDefinition(this.props.output.type, (definition) => {
            this.setState({typeDefinition: definition.requested_configuration});
            this.refs.configurationForm.open();
        });
    },
    _handleSubmit(data) {
        this.props.onUpdate(this.props.output, data);
    },
    render() {
        var typeDefinition = this.state.typeDefinition;
        var output = this.props.output;
        var configurationForm = (typeDefinition ?
            <ConfigurationForm ref="configurationForm" key={"configuration-form-output-"+output.id} configFields={this.state.typeDefinition}
                               title={"Editing Output " + output.title}
                               typeName={output.type}
                               helpBlock={<p className="help-block">{"Select a name of your new output that describes it."}</p>}
                               submitAction={this._handleSubmit} values={output.configuration} titleValue={output.title}/>
            : ""
        );
        return (
            <span>
                <Button className="btn btn-success btn-xs" onClick={this.handleClick.bind(null, output)}>
                    <i className="fa fa-edit"></i> Edit
                </Button>
                {configurationForm}
            </span>
        );
    }
});
module.exports = EditOutputButton;