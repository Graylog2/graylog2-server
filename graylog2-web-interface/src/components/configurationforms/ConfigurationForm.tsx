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
import * as React from 'react';
import type { ForwardedRef } from 'react';
import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { ConfigurationFormField, TitleField } from 'components/configurationforms';

import { FIELD_TYPES_WITH_ENCRYPTION_SUPPORT } from './types';
import type { ConfigurationFormData, ConfigurationField, ConfigurationFieldValue, EncryptedFieldValue, FieldValue, ConfigurationFieldWithEncryption } from './types';

type Props<Configuration extends object> = {
  cancelAction?: () => void,
  configFields?: {
    [key: string]: ConfigurationField,
  },
  children?: React.ReactNode,
  titleHelpText?: string,
  includeTitleField?: boolean,
  submitAction: (data: ConfigurationFormData<Configuration>) => void,
  title?: string | React.ReactNode | null
  titleValue?: string,
  typeName?: string,
  values?: { [key:string]: any },
  wrapperComponent?: React.ComponentType<React.PropsWithChildren<{
    show: boolean,
    title: string | React.ReactNode | null,
    onCancel: () => void,
    onSubmitForm: () => void,
    submitButtonText: string
  }>>,
  submitButtonText?: string,
}

export type RefType<Configuration extends object> = {
  open: () => void,
  getValue: () => ConfigurationFormData<Configuration>,
}

const defaultConfigFields = {};
const defaultCancelAction = () => {};
const defaultInitialValues = {};

const ConfigurationForm = forwardRef(<Configuration extends object>({
  cancelAction = defaultCancelAction,
  configFields = defaultConfigFields,
  children = null,
  titleHelpText = '',
  includeTitleField = true,
  submitAction,
  title = null,
  titleValue: initialTitleValue = '',
  typeName,
  values: initialValues = defaultInitialValues,
  wrapperComponent: WrapperComponent = BootstrapModalForm as Props<Configuration>['wrapperComponent'],
  submitButtonText,
} : Props<Configuration>, ref: React.ForwardedRef<RefType<Configuration>>) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(false);
  const [titleValue, setTitleValue] = useState(undefined);
  const [values, setValues] = useState<{[key:string]: any} | undefined>(undefined);
  const [fieldStates, setFieldStates] = useState<{[key:string]: any} | undefined>({});

  useEffect(() => {
    const defaultValues = {};

    if (configFields) {
      Object.keys(configFields).forEach((field) => {
        defaultValues[field] = configFields[field].default_value;
      });
    }

    setValues({ ...defaultValues, ...initialValues });
    setFieldStates({});
  }, [showConfigurationModal, configFields, initialValues]);

  useEffect(() => {
    setTitleValue(initialTitleValue);
  }, [initialTitleValue, showConfigurationModal]);

  const getFormData = (): ConfigurationFormData<Configuration> => {
    const data: ConfigurationFormData<Configuration> = {
      type: typeName,
      configuration: {},
    };

    if (includeTitleField) {
      data.title = titleValue;
    }

    Object.keys(configFields).forEach((fieldName) => {
      data.configuration[fieldName] = (values[fieldName] === undefined ? null : values[fieldName]);
    });

    return data;
  };

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

  const fieldIsEncrypted = (configField: ConfigurationField) : boolean => {
    const fieldSupportsEncryption = FIELD_TYPES_WITH_ENCRYPTION_SUPPORT.includes(
      (configField.type as unknown as typeof FIELD_TYPES_WITH_ENCRYPTION_SUPPORT[number]),
    );

    if (!fieldSupportsEncryption) {
      return false;
    }

    return (configField as ConfigurationFieldWithEncryption).is_encrypted;
  };

  const handleEncryptedFieldsBeforeSubmit = (data): ConfigurationFormData<Configuration> => {
    const configurationEntries = Object.entries(data.configuration).map(([fieldName, fieldValue] : [string, ConfigurationFieldValue]) => {
      const configField = configFields[fieldName as string];
      const fieldState = fieldStates[fieldName as string];

      if (fieldIsEncrypted(configField) && !fieldState?.dirty && fieldValue && (fieldValue as EncryptedFieldValue<FieldValue>).is_set !== undefined) {
        return [fieldName, { keep_value: true }];
      }

      return [fieldName, fieldValue];
    });

    return { ...data, configuration: Object.fromEntries(configurationEntries) };
  };

  const handleCancel = () => {
    setShowConfigurationModal(false);

    if (cancelAction) {
      cancelAction();
    }
  };

  const save = () => {
    const data = getFormData();

    submitAction(handleEncryptedFieldsBeforeSubmit(data));

    setShowConfigurationModal(false);
  };

  useImperativeHandle(ref, () => ({
    open() {
      setShowConfigurationModal(true);
    },
    getValue() {
      return getFormData();
    },
  }));

  const handleTitleChange = (_, value) => {
    setTitleValue(value);
  };

  const handleChange = (field: string, value: ConfigurationFieldValue, dirty: boolean = true) => {
    setValues({ ...values, ...{ [field]: value } });
    setFieldStates({ ...fieldStates, ...{ [field]: { dirty } } });
  };

  const renderConfigField = (configField, key, autoFocus) => {
    if (!values) return null;
    const value = values[key];

    return (
      <ConfigurationFormField key={key}
                              typeName={typeName}
                              configField={configField}
                              configKey={key}
                              configValue={value}
                              autoFocus={autoFocus}
                              dirty={fieldStates[key]?.dirty}
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
                  helpText={titleHelpText} />
    );

    shouldAutoFocus = false;
  }

  const sortedConfigFieldKeys = Object.keys(configFields).map((name, pos) => ({ name, pos })).sort(sortByPosOrOptionality);

  const renderedConfigFields = sortedConfigFieldKeys.map((key) => {
    const configField = renderConfigField(configFields[key.name], key.name, shouldAutoFocus);

    if (shouldAutoFocus) {
      shouldAutoFocus = false;
    }

    return configField;
  });

  return (
    <WrapperComponent show={showConfigurationModal}
                      title={title}
                      onCancel={handleCancel}
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

export default ConfigurationForm as <T extends object>(
  props: Props<T> & { ref?: ForwardedRef<RefType<T>> },
) => ReturnType<typeof ConfigurationForm>;
