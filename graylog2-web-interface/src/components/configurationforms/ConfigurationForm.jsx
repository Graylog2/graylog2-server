import $ from 'jquery';
import React from 'react';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import {
  BooleanField,
  DropdownField,
  ListField,
  NumberField,
  TextField,
  TitleField,
} from 'components/configurationforms';

const ConfigurationForm = React.createClass({
  propTypes: {
    cancelAction: React.PropTypes.func,
    children: React.PropTypes.node,
    helpBlock: React.PropTypes.node,
    includeTitleField: React.PropTypes.bool,
    submitAction: React.PropTypes.func.isRequired,
    title: React.PropTypes.node,
    titleValue: React.PropTypes.string,
    typeName: React.PropTypes.string,
    values: React.PropTypes.object,
  },

  getDefaultProps() {
    return {
      includeTitleField: true,
      titleValue: '',
      values: {},
    };
  },
  getInitialState() {
    return this._copyStateFromProps(this.props);
  },
  componentWillReceiveProps(props) {
    const newState = this._copyStateFromProps(props);
    const values = this.state ? this.state.values : {};
    newState.values = $.extend(newState.values, values);
    this.setState(newState);
  },
  getValue() {
    const data = {};
    const values = this.state.values;
    if (this.props.includeTitleField) {
      data.title = this.state.titleValue;
    }
    data.type = this.props.typeName;
    data.configuration = {};

    $.map(this.state.configFields, (field, name) => {
      // Replace undefined with null, as JSON.stringify will leave out undefined fields from the DTO sent to the server
      data.configuration[name] = (values[name] === undefined ? null : values[name]);
    });

    return data;
  },
  _copyStateFromProps(props) {
    const effectiveTitleValue = (this.state && this.state.titleValue !== undefined ? this.state.titleValue : props.titleValue);
    const defaultValues = {};

    if (props.configFields) {
      Object.keys(props.configFields).forEach((field) => {
        defaultValues[field] = props.configFields[field].default_value;
      });
    }

    return {
      configFields: $.extend({}, props.configFields),
      values: $.extend({}, defaultValues, props.values),
      titleValue: effectiveTitleValue,
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
    this.setState($.extend(this.getInitialState(), { titleValue: this.props.titleValue }));
    if (this.props.cancelAction) {
      this.props.cancelAction();
    }
  },
  _handleTitleChange(field, value) {
    this.setState({ titleValue: value });
  },
  _handleChange(field, value) {
    const values = this.state.values;
    values[field] = value;
    this.setState({ values: values });
  },
  _renderConfigField(configField, key, autoFocus) {
    const value = this.state.values[key];
    const typeName = this.props.typeName;
    const elementKey = `${typeName}-${key}`;

    switch (configField.type) {
      case 'text':
        return (<TextField key={elementKey} typeName={typeName} title={key} field={configField}
                           value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
      case 'number':
        return (<NumberField key={elementKey} typeName={typeName} title={key} field={configField}
                             value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
      case 'boolean':
        return (<BooleanField key={elementKey} typeName={typeName} title={key} field={configField}
                              value={value} onChange={this._handleChange} autoFocus={autoFocus} />);
      case 'dropdown':
        return (<DropdownField key={elementKey} typeName={typeName} title={key} field={configField}
                               value={value} onChange={this._handleChange} autoFocus={autoFocus} addPlaceholder />);
      case 'list':
        return (<ListField key={elementKey} typeName={typeName} title={key} field={configField}
                           value={value} onChange={this._handleChange} autoFocus={autoFocus} addPlaceholder />);
      default:
        return null;
    }
  },
  render() {
    const typeName = this.props.typeName;
    const title = this.props.title;
    const helpBlock = this.props.helpBlock;

    let shouldAutoFocus = true;
    let titleElement;
    if (this.props.includeTitleField) {
      titleElement = (<TitleField key={`${typeName}-title`} typeName={typeName} value={this.state.titleValue}
                                  onChange={this._handleTitleChange} helpBlock={helpBlock} />);
      shouldAutoFocus = false;
    }

    const configFieldKeys = $.map(this.state.configFields, (v, k) => k).sort(this._sortByOptionality);
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
                          onCancel={this._closeModal}
                          onSubmitForm={this._save}
                          submitButtonText="Save">
        <fieldset>
          <input type="hidden" name="type" value={typeName} />
          {this.props.children}
          {titleElement}
          {configFields}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default ConfigurationForm;
