import $ from 'jquery';
import React from 'react';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { BooleanField, DropdownField, NumberField, TextField } from 'components/configurationforms';

const ConfigurationForm = React.createClass({
  getInitialState() {
    return this._copyStateFromProps(this.props);
  },
  componentWillMount() {
    this.setState({values: $.extend({}, this.props.values)});
  },
  componentWillReceiveProps(props) {
    this.setState(this._copyStateFromProps(props));
  },
  getDefaultProps() {
    return {
      values: {},
      includeTitleField: true,
      titleValue: '',
    };
  },
  getValue() {
    const data = {};
    const values = this.state.values;
    if (this.props.includeTitleField) {
      data.title = this.state.titleValue;
    }
    data.type = this.props.typeName;
    data.configuration = {};

    $.map(this.state.configFields, function(field, name) {
      data.configuration[name] = (values[name] === undefined  || values === null || String(values[name]).trim() === "" ? field.default_value : values[name]);
    });

    return data;
  },
  _copyStateFromProps(props) {
    return {
      configFields: $.extend({}, props.configFields),
      values: $.extend({}, props.values),
      titleValue: props.titleValue,
    };
  },
  _sortByOptionality(x1, x2) {
    return (this.state.configFields[x1].is_optional - this.state.configFields[x2].is_optional);
  },
  _save() {
    const data = this.getValue();

    this.props.submitAction(data);
    this.refs.modal.close();
  },
  open() {
    this.refs.modal.open();
  },
  _closeModal() {
    if (this.props.cancelAction) {
      this.props.cancelAction();
    }
  },
  _handleTitleChange(field, value) {
    this.setState({titleValue: value});
  },
  _handleChange(field, value) {
    const values = this.state.values;
    values[field] = value;
    this.setState({values: values});
  },
  _renderConfigField(configField, key, autoFocus) {
    const value = this.state.values[key];
    const typeName = this.props.typeName;

    switch(configField.type) {
      case "text":
        return (<TextField key={typeName + "-" + key} typeName={typeName} title={key} field={configField}
                           value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
      case "number":
        return (<NumberField key={typeName + "-" + key} typeName={typeName} title={key} field={configField}
                             value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
      case "boolean":
        return (<BooleanField key={typeName + "-" + key} typeName={typeName} title={key} field={configField}
                              value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
      case "dropdown":
        return (<DropdownField key={typeName + "-" + key} typeName={typeName} title={key} field={configField}
                               value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
    }
  },
  render() {
    const typeName = this.props.typeName;
    const title = this.props.title;
    const helpBlock = this.props.helpBlock;
    const titleField = {is_optional: false, attributes: [], human_name: 'Title', description: helpBlock};

    let shouldAutoFocus = true;
    let titleElement;
    if (this.props.includeTitleField) {
      titleElement = (<TextField key={typeName + "-title"} typeName={typeName} title="title" field={titleField}
                                 value={this.state.titleValue} onChange={this._handleTitleChange} autoFocus />);
      shouldAutoFocus = false;
    }

    const configFieldKeys = $.map(this.state.configFields, (v,k) => {return k;}).sort(this._sortByOptionality);
    const configFields = configFieldKeys.map((key) => {
      const configField = this._renderConfigField(this.state.configFields[key], key, shouldAutoFocus);
      if (shouldAutoFocus) {
        shouldAutoFocus = false;
      }
      return configField;
    });

    return (
      <BootstrapModalForm ref="modal"
                          title={title}
                          onModalClose={this._closeModal}
                          onSubmitForm={this._save}
                          submitButtonText="Save">
        <fieldset>
          <input type="hidden" name="type" value={typeName} />
          {titleElement}
          {this.props.children}
          {configFields}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default ConfigurationForm;
