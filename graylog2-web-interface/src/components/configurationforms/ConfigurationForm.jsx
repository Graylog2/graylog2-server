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
  static propTypes = {
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
  };

  static defaultProps = {
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

  constructor(props) {
    super(props);
    this.state = this._copyStateFromProps(this.props);
  }

  // eslint-disable-next-line camelcase
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
      data.configuration[name] = (values[name] === undefined ? null : values[name]);
    });

    return data;
  };

  _copyStateFromProps = (props) => {
    const { titleValue } = this.state || {};
    const effectiveTitleValue = (titleValue !== undefined ? titleValue : props.titleValue);
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

  _save = () => {
    const data = this.getValue();

    const { submitAction } = this.props;

    submitAction(data);

    if (this.modal && this.modal.close) {
      this.modal.close();
    }
  };

  open = () => {
    if (this.modal && this.modal.open) {
      this.modal.open();
    }
  };

  _closeModal = () => {
    const { cancelAction, titleValue } = this.props;

    this.setState($.extend(this._copyStateFromProps(this.props), { titleValue: titleValue }));

    if (cancelAction) {
      cancelAction();
    }
  };

  _handleTitleChange = (field, value) => {
    this.setState({ titleValue: value });
  };

  _handleChange = (field, value) => {
    const { values } = this.state;

    values[field] = value;
    this.setState({ values: values });
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
                              onChange={this._handleChange} />
    );
  };

  render() {
    const { typeName, title, helpBlock, wrapperComponent: WrapperComponent = BootstrapModalForm, includeTitleField, children } = this.props;

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
      <WrapperComponent ref={(modal) => { this.modal = modal; }}
                        title={title}
                        onCancel={this._closeModal}
                        onSubmitForm={this._save}
                        submitButtonText="Save">
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

export default ConfigurationForm;
