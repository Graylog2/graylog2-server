'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');
var $ = require('jquery'); // excluded and shimed

var CreateOutputDropdown = React.createClass({
    PLACEHOLDER: "placeholder",
    getInitialState() {
        return {
            typeDefinition: [],
            typeName: this.PLACEHOLDER
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
    },
    render() {
        var outputTypes = $.map(this.props.types, this._formatOutputType);
        return (
            <div>
                <div className="form-group">
                    <div className="row">
                        <div className="col-md-2">
                            <select id="input-type" defaultValue={this.PLACEHOLDER} value={this.state.typeName} onChange={this._onTypeChange} className="form-control">
                                <option value={this.PLACEHOLDER} disabled>--- Select Output Type ---</option>
                                {outputTypes}
                            </select>
                        </div>
                        <button className="btn btn-success" disabled={this.state.typeName === this.PLACEHOLDER} onClick={this._openModal}>Launch new output</button>
                    </div>


                </div>

                <ConfigurationForm ref="configurationForm" key="configuration-form-output" configFields={this.state.typeDefinition} title="Create new Output"
                                   helpBlock={<p className="help-block">{"Select a name of your new output that describes it."}</p>}
                                   typeName={this.state.typeName}
                                   submitAction={this.props.onSubmit} />
            </div>
        );
    },
    _openModal(evt) {
        if (this.state.typeName !== this.PLACEHOLDER && this.state.typeName !== "") {
            this.refs.configurationForm.open();
        }
    },
    _formatOutputType(title, typeName) {
        return (<option key={typeName} value={typeName}>{title}</option>);
    },
    _onTypeChange(evt) {
        var outputType = evt.target.value;
        this.setState({typeName: evt.target.value});
        this.props.getTypeDefinition(outputType, (definition) => {
            this.setState({typeDefinition: definition.requested_configuration});
        });
    },
});

module.exports = CreateOutputDropdown;