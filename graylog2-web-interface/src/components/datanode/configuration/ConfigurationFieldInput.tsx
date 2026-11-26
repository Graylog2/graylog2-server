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

import { Badge, Button, Input } from 'components/bootstrap';
import { Icon, RelativeTime, Select, Tooltip } from 'components/common';

import type { ConfigurationValues, ConfigurationParameter, ConfigurationSource } from './types';
import ConfigurationQuantityInput from './ConfigurationQuantityInput';
import { changeQuantityAmount, changeQuantityUnit, getQuantity } from './configurationQuantities';
import type { QuantityChange } from './configurationQuantities';
import {
  applyConfigurationValue,
  getConfigurationValueSource,
  getDisplayedConfigurationValue,
  trimListEntries,
} from './configurationFields';

const FieldDetail = styled.span`
  display: block;
  margin-top: ${({ theme }) => theme.spacings.xs};
`;

const RestartBadge = styled(Badge)`
  margin-left: ${({ theme }) => theme.spacings.xs};
  vertical-align: middle;
`;

const sourceDescriptions: Partial<Record<ConfigurationSource, { label: string; description: string }>> = {
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
    label: 'This field',
    description: 'This value was set here and takes precedence over all other sources.',
  },
};

const restartBadges = {
  restart: { color: 'gray', label: 'Requires restart', title: 'Takes effect once the Data Node restarts.' },
  live: { color: 'primary', label: 'Applies without restart', title: 'Does not need a Data Node restart to take effect.' },
} as const;

type Props = {
  parameter: ConfigurationParameter;
  canEdit: boolean;
  canDelete: boolean;
  selectedUnit: string | undefined;
  onSelectUnit: (unit: string) => void;
};

const ConfigurationFieldInput = ({ parameter, canEdit, canDelete, selectedUnit, onSelectUnit }: Props) => {
  const { values, initialValues, errors, touched, isSubmitting, submitCount, setValues, setTouched } =
    useFormikContext<ConfigurationValues>();
  const { name, label, description, type, units, constraints, requires_restart, set_by, set_at } = parameter;
  const restartBadge = requires_restart ? restartBadges.restart : restartBadges.live;
  const draftValue = values[name];
  const selectableValues = Object.entries(constraints?.values ?? {});
  const error = touched[name] || submitCount > 0 ? errors[name] : undefined;
  const isResetPending = draftValue === null;
  const displayedValue = getDisplayedConfigurationValue(parameter, draftValue);
  const value = displayedValue ?? '';
  const placeholder = selectableValues.length > 0 || type === 'boolean' ? 'Select a value' : 'Enter a value';
  const source = value !== '' ? getConfigurationValueSource(parameter, draftValue, initialValues[name]) : undefined;
  const sourceDescription = source ? sourceDescriptions[source] : undefined;
  const disabled = !canEdit || isSubmitting;
  const quantity = getQuantity(parameter, displayedValue, selectedUnit);
  const unitLabel = quantity ? units[quantity.unit] : undefined;
  const helpId = `${name}-help`;
  const errorId = `${name}-error`;
  const describedBy = [helpId, error && errorId].filter(Boolean).join(' ');
  const onBlur = () => setTouched({ ...touched, [name]: true });
  const setValue = (nextValue: string) =>
    setValues(
      (currentValues) => applyConfigurationValue(currentValues, initialValues, parameter, nextValue),
      submitCount > 0,
    );
  const applyQuantityChange = ({ value: nextValue, selectedUnit: nextUnit }: QuantityChange) => {
    if (nextUnit !== undefined) onSelectUnit(nextUnit);
    if (nextValue !== undefined) setValue(nextValue);
  };

  const resetValue = async () => {
    await setValues((currentValues) => ({ ...currentValues, [name]: null }), true);
    document.getElementById(name)?.focus();
  };

  const resetButton =
    parameter.set_value != null && canEdit ? (
      <Button
        type="button"
        aria-label={`Reset value for ${label}`}
        disabled={disabled || !canDelete || isResetPending}
        onClick={resetValue}>
        Reset value
      </Button>
    ) : undefined;

  const nativeControlProps = {
    value,
    placeholder,
    disabled,
    'aria-describedby': describedBy,
    'aria-invalid': !!error,
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => setValue(event.target.value),
    onBlur,
  };

  const getControl = () => {
    if (quantity) {
      return {
        control: (
          <ConfigurationQuantityInput
            id={name}
            label={label}
            units={units}
            amount={quantity.amount}
            unit={quantity.unit}
            placeholder={placeholder}
            disabled={disabled}
            describedBy={describedBy}
            invalid={!!error}
            onAmountChange={(amount) => applyQuantityChange(changeQuantityAmount(quantity, amount))}
            onUnitChange={(unit) => applyQuantityChange(changeQuantityUnit(quantity, unit))}
            onBlur={onBlur}
            buttonAfter={resetButton}
          />
        ),
      };
    }

    if (selectableValues.length > 0) {
      return {
        buttonAfter: resetButton,
        control: (
          <Select
            inputId={name}
            name={name}
            aria-label={label}
            aria-invalid={!!error}
            aria-errormessage={error ? errorId : undefined}
            inputProps={{ 'aria-describedby': helpId }}
            multi={type === 'list'}
            options={selectableValues.map(([optionValue, optionLabel]) => ({ value: optionValue, label: optionLabel }))}
            placeholder={placeholder}
            value={type === 'list' ? trimListEntries(value) : value}
            onChange={setValue}
            onBlur={onBlur}
            disabled={disabled}
          />
        ),
      };
    }

    if (type === 'boolean') {
      return {
        ...nativeControlProps,
        type: 'select',
        buttonAfter: resetButton,
        control: (
          <>
            <option value="" disabled>
              {placeholder}
            </option>
            <option value="true">Enabled</option>
            <option value="false">Disabled</option>
          </>
        ),
      };
    }

    return { ...nativeControlProps, type: type === 'integer' ? 'number' : 'text', buttonAfter: resetButton };
  };

  const { control, ...controlProps } = getControl();

  return (
    <Input
      id={name}
      name={name}
      label={unitLabel ? `${label} (${unitLabel})` : label}
      {...controlProps}
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
                {' · Current source: '}
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
                {source === 'database' && set_by && (
                  <>
                    {` · Set by ${set_by}`}
                    {set_at && (
                      <>
                        {' '}
                        <RelativeTime dateTime={set_at} />
                      </>
                    )}
                  </>
                )}
              </>
            )}
            <RestartBadge color={restartBadge.color} variant="light" size="sm" title={restartBadge.title}>
              {restartBadge.label}
            </RestartBadge>
          </FieldDetail>
        </span>
      }>
      {control}
    </Input>
  );
};

export default ConfigurationFieldInput;
