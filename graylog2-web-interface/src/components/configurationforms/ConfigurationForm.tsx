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
import * as React from 'react';
import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { ConfigurationFormField, TitleField } from 'components/configurationforms';

import type { ConfigurationField } from './types';

type Props = {
  cancelAction: () => void,
  configFields?: {
    [key: string]: ConfigurationField,
  },
  children: React.ReactNode | null,
  helpBlock: React.ReactNode | null,
  includeTitleField: boolean,
  submitAction: (data: any) => void,
  title: string | React.ReactNode | null,
  titleValue: string,
  typeName?: string,
  values: { [key:string]: any },
  wrapperComponent: React.ComponentType<any>,
  submitButtonText: string,
}

const ConfigurationForm = forwardRef<{}, Props>(({
  cancelAction,
  configFields,
  children,
  helpBlock,
  includeTitleField,
  submitAction,
  title,
  titleValue: initialTitleValue,
  typeName,
  values: initialValues,
  wrapperComponent: WrapperComponent,
  submitButtonText,
} : Props, ref: typeof ConfigurationForm) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(false);
  const [titleValue, setTitleValue] = useState(undefined);
  const [values, setValues] = useState<{[key:string]: any} | undefined>(undefined);

  useEffect(() => {
    if (!titleValue) {
      setTitleValue(initialTitleValue);
    }
  }, [titleValue, initialTitleValue]);

  useEffect(() => {
    const defaultValues = {};

    if (configFields) {
      Object.keys(configFields).forEach((field) => {
        defaultValues[field] = configFields[field].default_value;
      });
    }

    setValues({ ...defaultValues, ...initialValues });
  }, [configFields, initialValues]);

  const getValue = () => {
    const data: { title?: string, type?: string, configuration?: {[key:string]: any} } = { type: typeName, configuration: { } };

    if (includeTitleField) {
      data.title = titleValue;
    }

    $.map(configFields, (_, name) => {
      // Replace undefined with null, as JSON.stringify will leave out undefined fields from the DTO sent to the server
      data.configuration[name] = (values[name] === undefined ? null : values[name]);
    });

    return data;
  };

  useImperativeHandle(ref, () => ({
    getValue() {
      getValue();
    },
  }));

  const sortByPosOrOptionality = (x1, x2) => {
    const DEFAULT_POSITION = 100; // corresponds to ConfigurationField.java
    const x1pos = configFields[x1.name].position || DEFAULT_POSITION;
    const x2pos = configFields[x2.name].position || DEFAULT_POSITION;

    let diff = x1pos - x2pos;

    if (!diff) {
      const isOptionalToNumber = (optional: boolean) : number => (optional ? 1 : 0);

      diff = isOptionalToNumber(configFields[x1.name].is_optional) - isOptionalToNumber(configFields[x2.name].is_optional);
    }

    if (!diff) {
      // Sort equal fields stably
      diff = x1.pos - x2.pos;
    }

    return diff;
  };

  const closeModal = () => {
    setTitleValue(titleValue);
    setShowConfigurationModal(false);

    if (cancelAction) {
      cancelAction();
    }
  };

  const save = () => {
    const data = getValue();

    submitAction(data);

    closeModal();
  };

  useImperativeHandle(ref, () => ({
    open() {
      setShowConfigurationModal(true);
    },
  }));

  const handleTitleChange = (_, value) => {
    setTitleValue(value);
  };

  const handleChange = (field: string, value) => {
    setValues({ ...values, ...{ [field]: value } });
  };

  const renderConfigField = (configField, key, autoFocus, buttonAfter = null) => {
    if (!values) return null;
    const value = values[key];

    return (
      <ConfigurationFormField key={key}
                              typeName={typeName}
                              configField={configField}
                              configKey={key}
                              configValue={value}
                              autoFocus={autoFocus}
                              buttonAfter={buttonAfter}
                              onChange={handleChange} />
    );
  };

  let shouldAutoFocus = true;
  let titleElement;

  if (includeTitleField) {
    titleElement = (
      <TitleField key={`${typeName}-title`}
                  typeName={typeName}
                  value={titleValue}
                  onChange={handleTitleChange}
                  helpBlock={helpBlock} />
    );

    shouldAutoFocus = false;
  }

  const configFieldKeys = $.map(configFields, (_, name) => name)
    .map((name, pos) => ({ name: name, pos: pos }))
    .sort(sortByPosOrOptionality);

  const renderedConfigFields = configFieldKeys.map((key) => {
    const configField = renderConfigField(configFields[key.name], key.name, shouldAutoFocus);

    if (shouldAutoFocus) {
      shouldAutoFocus = false;
    }

    return configField;
  });

  return (
    <WrapperComponent show={showConfigurationModal}
                      title={title}
                      onCancel={closeModal}
                      onSubmitForm={save}
                      submitButtonText={submitButtonText}>
      <fieldset>
        <input type="hidden" name="type" value={typeName} />
        {children}
        {titleElement}
        {renderedConfigFields}
      </fieldset>
    </WrapperComponent>
  );
});

ConfigurationForm.propTypes = {
  cancelAction: PropTypes.func,
  configFields: PropTypes.object,
  children: PropTypes.node,
  helpBlock: PropTypes.node,
  includeTitleField: PropTypes.bool,
  submitAction: PropTypes.func.isRequired,
  title: PropTypes.node,
  titleValue: PropTypes.string,
  typeName: PropTypes.string,
  values: PropTypes.object,
  wrapperComponent: PropTypes.elementType,
  submitButtonText: PropTypes.string.isRequired,
};

ConfigurationForm.defaultProps = {
  cancelAction: () => {},
  configFields: undefined,
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
