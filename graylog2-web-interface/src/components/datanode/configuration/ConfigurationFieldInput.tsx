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
import { Icon, Select, Tooltip } from 'components/common';
import VisuallyHidden from 'components/common/VisuallyHidden';

import type { ConfigurationFormValues, ConfigurationParameter, ConfigurationSource } from './types';
import ConfigurationQuantityInput from './ConfigurationQuantityInput';
import { getQuantityUnitLabel } from './configurationQuantities';

const FieldDetail = styled.span`
  display: block;
  margin-top: ${({ theme }) => theme.spacings.xs};
`;

const FieldLabel = styled.span`
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const sourceDescriptions: Record<ConfigurationSource, string> = {
  default: 'the built-in default',
  config_file: 'the configuration file',
  environment: 'an environment variable',
  system_property: 'a system property',
  database: 'a stored value',
  unknown: 'an unknown source',
};

type Props = {
  parameter: ConfigurationParameter;
  canEdit: boolean;
  canDelete: boolean;
  restartDescription: string;
};

const ConfigurationFieldInput = ({ parameter, canEdit, canDelete, restartDescription }: Props) => {
  const { values, errors, touched, isSubmitting, submitCount, setValues, setTouched } =
    useFormikContext<ConfigurationFormValues>();
  const { name, label, description, type, units, constraints } = parameter;
  const selectableValues = Object.entries(constraints?.values ?? {});
  const error = touched[name] || submitCount > 0 ? errors[name] : undefined;
  const rawValue = values[name];
  const value = rawValue ?? '';
  const disabled = !canEdit || isSubmitting;
  const hasUnits = Object.keys(units ?? {}).length > 0;
  const isQuantity = hasUnits && (type === 'size' || type === 'duration');
  const unitLabel = isQuantity ? getQuantityUnitLabel(rawValue, units) : undefined;
  const labelId = `${name}-label`;
  const restartId = `${name}-restart`;
  const helpId = `${name}-help`;
  const errorId = `${name}-error`;
  const describedBy = [helpId, parameter.requires_restart && restartId, error && errorId].filter(Boolean).join(' ');
  const onBlur = () => setTouched({ ...touched, [name]: true });
  // Parameter names contain dots; keep literal keys instead of Formik object paths.
  const setValue = (nextValue: string) =>
    setValues((currentValues) => ({ ...currentValues, [name]: nextValue }), submitCount > 0);

  const removeStoredValue = () =>
    setValues((currentValues) => {
      const nextValues = { ...currentValues };
      delete nextValues[name];

      return nextValues;
    }, submitCount > 0);

  const isPendingRestart =
    parameter.set_value !== null &&
    parameter.node_value !== null &&
    parameter.set_value !== parameter.node_value &&
    parameter.requires_restart;

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
          value={rawValue}
          disabled={disabled}
          labelledBy={labelId}
          describedBy={describedBy}
          invalid={!!error}
          onChange={setValue}
          onBlur={onBlur}
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
          placeholder="No value set"
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
            No value set
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
      label={
        <FieldLabel>
          <span id={labelId}>{unitLabel ? `${label} (${unitLabel})` : label}</span>
          {parameter.requires_restart && (
            <Tooltip label={restartDescription} withArrow>
              <span role="note" aria-label={restartDescription}>
                <Badge color="warning" variant="light" size="sm">
                  <Icon name="restart_alt" size="sm" />
                  Restart required
                </Badge>
                <VisuallyHidden id={restartId}>{restartDescription}</VisuallyHidden>
              </span>
            </Tooltip>
          )}
        </FieldLabel>
      }
      aria-labelledby={labelId}
      aria-describedby={describedBy}
      aria-invalid={!!error}
      {...getControlProps()}
      disabled={disabled}
      placeholder="No value set"
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
          </FieldDetail>
          {parameter.source && (
            <FieldDetail>
              {parameter.set_value === null
                ? `Currently taken from ${sourceDescriptions[parameter.source]}.`
                : `Stored value${parameter.set_by ? `, set by ${parameter.set_by}` : ''}.`}
            </FieldDetail>
          )}
          {isPendingRestart && (
            <FieldDetail>
              This Data Node still runs <code>{parameter.node_value}</code> and picks the stored value up when it
              restarts.
            </FieldDetail>
          )}
          {parameter.set_value !== null && (
            <FieldDetail>
              <Button
                bsSize="xsmall"
                aria-label={`Remove stored value for ${label}`}
                disabled={disabled || !canDelete}
                onClick={removeStoredValue}>
                Remove stored value
              </Button>
              {parameter.node_value !== null && (
                <>
                  {' '}
                  Falls back to <code>{parameter.node_value}</code>.
                </>
              )}
            </FieldDetail>
          )}
        </span>
      }>
      {renderControl()}
    </Input>
  );
};

export default ConfigurationFieldInput;
