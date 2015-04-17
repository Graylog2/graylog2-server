'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');
var TextField = require('./TextField');
var NumberField = require('./NumberField');
var BooleanField = require('./BooleanField');
var DropdownField = require('./DropdownField');

var ConfigurationForm = React.createClass({
    getInitialState() {
        return {
            configFields: this.props.configFields,
            title: this.props.title,
            typeName: this.props.typeName,
            formId: this.props.formId,
            submitAction: this.props.submitAction,
            helpBlock: this.props.helpBlock,
            values: {}
        };
    },
    componentDidMount() {},
	componentWillReceiveProps(props) {
		this.setState(props);
	},
    render() {
        var typeName = this.state.typeName;
        var configFields = $.map(this.state.configFields, this._renderConfigField);
        var title = this.state.title;
        var helpBlock = this.state.helpBlock;
        return (
            <form onSubmit={this.handleSubmit} validate>
                <div id={this.state.formId} className="configuration-form modal fade" data-inputtype={typeName} tabIndex="-1" role="dialog" aria-hidden="false">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                                <h2 className="modal-title">
                                    <i className="fa fa-signin"></i>
                                    {title}
                                </h2>
                            </div>

                            <div className="modal-body">
                                <input type="hidden" name="type" value={typeName} />

                                <label htmlFor={"title-" + typeName}>Title</label>
                                <input id={"title-" + typeName} name="title" onChange={this.handleTitleChange} required="true" type="text" className="input-xlarge form-control" />
                                    {helpBlock}
                                    {configFields}
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn" data-dismiss="modal">Close</button>
                                <button type="submit" onClick={this.handleSubmit} className="btn btn-primary" data-type={typeName}>
                                    <i className="fa fa-rocket"></i>
                                    Save
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        );
    },
    handleSubmit(evt) {
        evt.preventDefault();
        var values = this.state.values;
        var data = {title: values.title,
            type: this.state.typeName,
            configuration: {}};
        $.map(this.state.configFields, function(field, name) {
            data.configuration[name] = (values[name] || field.default_value);
        });
        this.props.submitAction(data);
    },
    handleTitleChange(evt) {
        this.handleChange('title', evt.target.value);
    },
    handleChange(field, value) {
        var values = this.state.values;
        values[field] = value;
        this.setState({values: values});
    },
    _renderConfigField(configField, key) {
        switch(configField.type) {
            case "text":
                return (<TextField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} onChange={this.handleChange}/>);
            case "number":
                return (<NumberField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} onChange={this.handleChange}/>);
            case "boolean":
                return (<BooleanField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} onChange={this.handleChange}/>);
            case "dropdown":
                return (<DropdownField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} onChange={this.handleChange}/>);
        }
    }
});

module.exports = ConfigurationForm;
