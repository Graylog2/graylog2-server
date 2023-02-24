/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import $ from 'jquery';
import PropTypes from 'prop-types';
import React from 'react';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { ConfigurationFormField, TitleField } from 'components/configurationforms';

class ConfigurationForm extends React.Component {
  constructor(props) {
    super(props);

    this.state = { ...this._copyStateFromProps(this.props), showConfigurationModal: false };
  }

  UNSAFE_componentWillReceiveProps(props) {
    const { values = {} } = this.state || {};
    const newState = this._copyStateFromProps(props);

    newState.values = $.extend(newState.values, values);
    this.setState(newState);
  }

  getValue = () => {
    const data = {};
    const { values } = this.state;
    const { includeTitleField, typeName } = this.props;
    const { configFields, titleValue } = this.state;

    if (includeTitleField) {
      data.title = titleValue;
    }

    data.type = typeName;
    data.configuration = {};

    $.map(configFields, (field, name) => {
      // Replace undefined with null, as JSON.stringify will leave out undefined fields from the DTO sent to the server
      const fieldValue = values[name];
      data.configuration[name] = (fieldValue === undefined ? null : fieldValue);
    });

    return data;
  };

  _copyStateFromProps = (props) => {
    const { titleValue } = this.state || {};
    const effectiveTitleValue = (titleValue !== undefined ? titleValue : props.titleValue);
    const defaultValues = {};

    if (props.configFields) {
      Object.keys(props.configFields).forEach((field) => {
        const configField = props.configFields[field];

        defaultValues[field] = configField.default_value;
      });
    }

    return {
      configFields: $.extend({}, props.configFields),
      values: $.extend({}, defaultValues, props.values),
      titleValue: effectiveTitleValue,
    };
  };

  _sortByPosOrOptionality = (x1, x2) => {
    const { configFields } = this.state;
    const DEFAULT_POSITION = 100; // corresponds to ConfigurationField.java
    const x1pos = configFields[x1.name].position || DEFAULT_POSITION;
    const x2pos = configFields[x2.name].position || DEFAULT_POSITION;

    let diff = x1pos - x2pos;

    if (!diff) {
      diff = configFields[x1.name].is_optional - configFields[x2.name].is_optional;
    }

    if (!diff) {
      // Sort equal fields stably
      diff = x1.pos - x2.pos;
    }

    return diff;
  };

  _handleEncryptedFieldsBeforeSubmit = (data) => {
    const { configFields } = this.state;

    const oldConfiguration = data.configuration;

    const newConfiguration = {};

    $.map(oldConfiguration, (fieldValue, fieldName) => {
      const configField = configFields[fieldName];

      if (configField.is_encrypted && !configField.dirty && fieldValue && fieldValue.is_set !== undefined) {
        newConfiguration[fieldName] = { keep_value: true };
      }
    });

    return { ...data, configuration: { ...oldConfiguration, ...newConfiguration } };
  };

  _save = () => {
    const data = this.getValue();

    const { titleValue, submitAction } = this.props;

    submitAction(this._handleEncryptedFieldsBeforeSubmit(data));

    // console.log('===copy state from props after submit', this._copyStateFromProps(this.props));

    // this.setState(this._copyStateFromProps(this.props));

    // console.log('===after submit state', this.state);

    this.setState($.extend(this._copyStateFromProps(this.props), { showConfigurationModal: false }));
  };

  // eslint-disable-next-line react/no-unused-class-component-methods
  open = () => {
    this.setState({ showConfigurationModal: true });
  };

  onCancel = () => {
    const { cancelAction, titleValue } = this.props;

    this.setState($.extend(this._copyStateFromProps(this.props), { titleValue: titleValue, showConfigurationModal: false }));

    if (cancelAction) {
      cancelAction();
    }
  };

  _handleTitleChange = (field, value) => {
    this.setState({ titleValue: value });
  };

  _handleChange = (field, value) => {
    const { configFields, values } = this.state;

    const configField = configFields[field];

    values[field] = value;

    this.setState({ values: values, configFields: { ...configFields, [field]: { ...configField, ...{ dirty: true } } } });
  };

  _renderConfigField = (configField, key, autoFocus) => {
    const { values } = this.state;
    const value = values[key];
    const { typeName } = this.props;

    return (
      <ConfigurationFormField key={key}
                              typeName={typeName}
                              configField={configField}
                              configKey={key}
                              configValue={value}
                              autoFocus={autoFocus}
                              dirty={configField.dirty}
                              onChange={this._handleChange} />
    );
  };

  render() {
    const {
      typeName,
      title,
      helpBlock,
      wrapperComponent: WrapperComponent = BootstrapModalForm,
      includeTitleField,
      children,
      submitButtonText,
    } = this.props;

    let shouldAutoFocus = true;
    let titleElement;

    if (includeTitleField) {
      const { titleValue } = this.state;

      titleElement = (
        <TitleField key={`${typeName}-title`}
                    typeName={typeName}
                    value={titleValue}
                    onChange={this._handleTitleChange}
                    helpBlock={helpBlock} />
      );

      shouldAutoFocus = false;
    }

    const { configFields } = this.state;
    const configFieldKeys = $.map(configFields, (field, name) => name)
      .map((name, pos) => ({ name: name, pos: pos }))
      .sort(this._sortByPosOrOptionality);

    const renderedConfigFields = configFieldKeys.map((key) => {
      const configField = this._renderConfigField(configFields[key.name], key.name, shouldAutoFocus);

      if (shouldAutoFocus) {
        shouldAutoFocus = false;
      }

      return configField;
    });

    return (
      <WrapperComponent show={this.state.showConfigurationModal}
                        title={title}
                        onCancel={this.onCancel}
                        onSubmitForm={this._save}
                        submitButtonText={submitButtonText}>
        <fieldset>
          <input type="hidden" name="type" value={typeName} />
          {children}
          {titleElement}
          {renderedConfigFields}
        </fieldset>
      </WrapperComponent>
    );
  }
}

ConfigurationForm.propTypes = {
  cancelAction: PropTypes.func,
  children: PropTypes.node,
  helpBlock: PropTypes.node,
  includeTitleField: PropTypes.bool,
  submitAction: PropTypes.func.isRequired,
  title: PropTypes.node,
  titleValue: PropTypes.string,
  typeName: PropTypes.string,
  // eslint-disable-next-line react/no-unused-prop-types
  values: PropTypes.object,
  wrapperComponent: PropTypes.elementType,
  submitButtonText: PropTypes.string.isRequired,
};

ConfigurationForm.defaultProps = {
  cancelAction: () => {},
  children: null,
  helpBlock: null,
  title: null,
  includeTitleField: true,
  titleValue: '',
  typeName: undefined,
  values: {},
  wrapperComponent: BootstrapModalForm,
};

export default ConfigurationForm;
