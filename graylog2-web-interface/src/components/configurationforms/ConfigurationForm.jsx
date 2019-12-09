import $ from 'jquery';
import PropTypes from 'prop-types';
import React from 'react';

import createReactClass from 'create-react-class';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { TitleField, ConfigurationFormField } from 'components/configurationforms';

const ConfigurationForm = createReactClass({
  displayName: 'ConfigurationForm',

  propTypes: {
    cancelAction: PropTypes.func,
    children: PropTypes.node,
    helpBlock: PropTypes.node,
    includeTitleField: PropTypes.bool,
    submitAction: PropTypes.func.isRequired,
    title: PropTypes.node,
    titleValue: PropTypes.string,
    typeName: PropTypes.string,
    values: PropTypes.object,
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
    const { values } = this.state;
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
    let diff = this.state.configFields[x1.name].is_optional - this.state.configFields[x2.name].is_optional;

    if (!diff) {
      // Sort equal fields stably
      diff = x1.pos - x2.pos;
    }

    return diff;
  },

  _save() {
    const data = this.getValue();

    this.props.submitAction(data);
    this.modal.close();
  },

  open() {
    this.modal.open();
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
    const { values } = this.state;
    values[field] = value;
    this.setState({ values: values });
  },

  _renderConfigField(configField, key, autoFocus) {
    const value = this.state.values[key];
    const { typeName } = this.props;

    return (
      <ConfigurationFormField key={key}
                              typeName={typeName}
                              configField={configField}
                              configKey={key}
                              configValue={value}
                              autoFocus={autoFocus}
                              onChange={this._handleChange} />
    );
  },

  render() {
    const { typeName, title, helpBlock, wrapperComponent = BootstrapModalForm } = this.props;

    let shouldAutoFocus = true;
    let titleElement;
    if (this.props.includeTitleField) {
      titleElement = (
        <TitleField key={`${typeName}-title`}
                    typeName={typeName}
                    value={this.state.titleValue}
                    onChange={this._handleTitleChange}
                    helpBlock={helpBlock} />
      );
      shouldAutoFocus = false;
    }

    const configFieldKeys = $.map(this.state.configFields, (field, name) => name)
      .map((name, pos) => ({ name: name, pos: pos }))
      .sort(this._sortByOptionality);

    const configFields = configFieldKeys.map((key) => {
      const configField = this._renderConfigField(this.state.configFields[key.name], key.name, shouldAutoFocus);
      if (shouldAutoFocus) {
        shouldAutoFocus = false;
      }
      return configField;
    });

    const WrapperComponent = wrapperComponent

    return (
      <WrapperComponent ref={(modal) => { this.modal = modal; }}
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
      </WrapperComponent>
    );
  },
});

export default ConfigurationForm;
