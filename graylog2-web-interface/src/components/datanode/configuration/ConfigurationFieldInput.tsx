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
import React from 'react';
import { useFormikContext } from 'formik';
import styled from 'styled-components';

import { Button, Input } from 'components/bootstrap';
import { Icon, Select, Tooltip } from 'components/common';

import type { ConfigurationFormValues, ConfigurationParameter, ConfigurationSource } from './types';
import ConfigurationQuantityInput from './ConfigurationQuantityInput';
import { getQuantityUnitLabel } from './configurationQuantities';
import { getDisplayedConfigurationValue } from './configurationFields';

const FieldDetail = styled.span`
  display: block;
  margin-top: ${({ theme }) => theme.spacings.xs};
`;

const sourceDescriptions: Record<ConfigurationSource, { label: string; description: string } | undefined> = {
  default: {
    label: 'Built-in default',
    description: 'This is the built-in default, used when no value has been explicitly configured.',
  },
  config_file: {
    label: 'Configuration file',
    description: 'This value was read from a configuration file on the Data Node.',
  },
  environment: {
    label: 'Environment variable',
    description: 'This value was read from an environment variable set for the Data Node process.',
  },
  system_property: {
    label: 'System property',
    description: 'This value was read from a Java system property (-D...) passed to the Data Node process at startup.',
  },
  database: {
    label: 'Saved value',
    description:
      'This value is saved in the application and takes precedence over configuration files, environment variables, and Java system properties.',
  },
  unknown: undefined,
};

type Props = {
  parameter: ConfigurationParameter;
  canEdit: boolean;
  canDelete: boolean;
};

const ConfigurationFieldInput = ({ parameter, canEdit, canDelete }: Props) => {
  const { values, initialValues, errors, touched, isSubmitting, submitCount, setValues, setTouched } =
    useFormikContext<ConfigurationFormValues>();
  const { name, label, description, type, units, constraints } = parameter;
  const draftValue = values[name];
  const selectableValues = Object.entries(constraints?.values ?? {});
  const error = touched[name] || submitCount > 0 ? errors[name] : undefined;
  const isResetPending = draftValue === null;
  const displayedValue = getDisplayedConfigurationValue(parameter, draftValue);
  const value = displayedValue ?? '';
  const placeholder = selectableValues.length > 0 || type === 'boolean' ? 'Select a value' : 'Enter a value';
  const source = isResetPending ? parameter.node_source : parameter.source;
  const hasMatchingSource = isResetPending || (draftValue === initialValues[name] && draftValue === parameter.value);
  const sourceDescription = value !== '' && hasMatchingSource && source ? sourceDescriptions[source] : undefined;
  const disabled = !canEdit || isSubmitting;
  const hasUnits = Object.keys(units ?? {}).length > 0;
  const isQuantity = hasUnits && (type === 'size' || type === 'duration');
  const unitLabel = isQuantity ? getQuantityUnitLabel(displayedValue, units) : undefined;
  const labelId = `${name}-label`;
  const helpId = `${name}-help`;
  const errorId = `${name}-error`;
  const describedBy = [helpId, error && errorId].filter(Boolean).join(' ');
  const onBlur = () => setTouched({ ...touched, [name]: true });
  const setValue = (nextValue: string) =>
    setValues(
      (currentValues) =>
        nextValue === getDisplayedConfigurationValue(parameter, currentValues[name])
          ? currentValues
          : { ...currentValues, [name]: nextValue },
      submitCount > 0,
    );

  const resetValue = async () => {
    await setValues((currentValues) => ({ ...currentValues, [name]: null }), true);
    document.getElementById(name)?.focus();
  };

  const resetButton =
    parameter.set_value != null ? (
      <Button
        type="button"
        aria-label={`Reset value for ${label}`}
        disabled={disabled || !canDelete || isResetPending}
        onClick={resetValue}>
        Reset value
      </Button>
    ) : undefined;

  const getControlProps = () => {
    if (isQuantity || selectableValues.length > 0) return { type: undefined, value: undefined };
    if (type === 'integer') return { type: 'number', value };
    if (type === 'boolean') return { type: 'select', value };

    return { type: 'text', value };
  };

  const renderControl = () => {
    if (isQuantity) {
      return (
        <ConfigurationQuantityInput
          id={name}
          label={label}
          units={units}
          value={displayedValue}
          placeholder={placeholder}
          disabled={disabled}
          labelledBy={labelId}
          describedBy={describedBy}
          invalid={!!error}
          onChange={setValue}
          onBlur={onBlur}
          buttonAfter={resetButton}
        />
      );
    }

    if (selectableValues.length > 0) {
      return (
        <Select
          inputId={name}
          name={name}
          aria-label={label}
          inputProps={{ 'aria-describedby': describedBy, 'aria-invalid': !!error }}
          multi={type === 'list'}
          options={selectableValues.map(([optionValue, optionLabel]) => ({ value: optionValue, label: optionLabel }))}
          placeholder={placeholder}
          value={value}
          onChange={setValue}
          onBlur={onBlur}
          disabled={disabled}
        />
      );
    }

    if (type === 'boolean') {
      return (
        <>
          <option value="" disabled>
            {placeholder}
          </option>
          <option value="true">Enabled</option>
          <option value="false">Disabled</option>
        </>
      );
    }

    return undefined;
  };

  return (
    <Input
      id={name}
      name={name}
      label={<span id={labelId}>{unitLabel ? `${label} (${unitLabel})` : label}</span>}
      aria-labelledby={labelId}
      aria-describedby={describedBy}
      aria-invalid={!!error}
      {...getControlProps()}
      disabled={disabled}
      buttonAfter={isQuantity ? undefined : resetButton}
      placeholder={placeholder}
      onChange={(event: React.ChangeEvent<HTMLInputElement>) => setValue(event.target.value)}
      onBlur={onBlur}
      error={
        error ? (
          <span id={errorId} role="alert">
            {error}
          </span>
        ) : undefined
      }
      help={
        <span id={helpId}>
          {description}
          <FieldDetail>
            <code>{name}</code>
            {sourceDescription && (
              <>
                {' · Source: '}
                {sourceDescription.label}
                <Tooltip label={sourceDescription.description} multiline maw={400}>
                  <Button
                    type="button"
                    bsStyle="transparent"
                    bsSize="xsmall"
                    aria-label={`About the value for ${label}`}>
                    <Icon name="help" type="regular" />
                  </Button>
                </Tooltip>
              </>
            )}
          </FieldDetail>
        </span>
      }>
      {renderControl()}
    </Input>
  );
};

export default ConfigurationFieldInput;
