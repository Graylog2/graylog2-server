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
            values: this.props.values || {},
            titleValue: this.props.titleValue
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
        var header = (
            <h2 className="modal-title">
                <i className="fa fa-signin"></i>
                {title}
            </h2>
        );
        var body = (
            <fieldset>
                <input type="hidden" name="type" value={typeName} />

                <label htmlFor={"title-" + typeName}>Title</label>
                <input id={"title-" + typeName} name="title" value={this.state.titleValue} onChange={this.handleTitleChange}
                       required="true" type="text" className="input-xlarge form-control" />
                {helpBlock}
                {configFields}
            </fieldset>
        );
        return (
            <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
                {header}
                {body}
            </BootstrapModal>
        );
    },
    _save(evt) {
        evt.preventDefault();
        var values = this.state.values;
        var data = {title: values.title,
            type: this.state.typeName,
            configuration: {}};
        $.map(this.state.configFields, function(field, name) {
            data.configuration[name] = (values[name] || field.default_value);
        });
        this.props.submitAction(data);
        this.refs.modal.close();
    },
    open() {
        this.refs.modal.open();
    },
    _closeModal() {
        this.refs.modal.close();
    },
    handleTitleChange(evt) {
        this.handleChange('title', evt.target.value);
        this.setState({titleValue: evt.target.value});
    },
    handleChange(field, value) {
        var values = this.state.values;
        values[field] = value;
        this.setState({values: values});
    },
    _renderConfigField(configField, key) {
        var value = this.state.values[key];
        switch(configField.type) {
            case "text":
                return (<TextField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} value={value} onChange={this.handleChange}/>);
            case "number":
                return (<NumberField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} value={value} onChange={this.handleChange}/>);
            case "boolean":
                return (<BooleanField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} value={value} onChange={this.handleChange}/>);
            case "dropdown":
                return (<DropdownField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} value={value} onChange={this.handleChange}/>);
        }
    }
});

module.exports = ConfigurationForm;
