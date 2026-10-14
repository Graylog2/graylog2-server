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

import type { ConfigurationField, ConfigurationFormValues } from './types';
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

const getControlProps = (type: ConfigurationField['type'], value: string) => {
  switch (type) {
    case 'heap':
      if (value === 'automatic') return { type: 'select', value };

      return { type: 'select', value: value === '' ? '' : 'custom' };
    case 'roles':
    case 'quantity':
      return { type: undefined, value: undefined };
    default:
      return { type, value };
  }
};

type Props = {
  field: ConfigurationField;
  canEdit: boolean;
  canDelete: boolean;
  inheritedValue?: string;
  restartDescription: string;
};

const ConfigurationFieldInput = ({
  field,
  canEdit,
  canDelete,
  inheritedValue = undefined,
  restartDescription,
}: Props) => {
  const { values, errors, touched, isSubmitting, submitCount, setValues, setTouched } =
    useFormikContext<ConfigurationFormValues>();
  const error = touched[field.key] || submitCount > 0 ? errors[field.key] : undefined;
  const isHeap = field.type === 'heap';
  const usesGlobalValue = !Object.hasOwn(values, field.key) && inheritedValue !== undefined;
  const value = values[field.key] ?? inheritedValue ?? '';
  const disabled = !canEdit || isSubmitting;
  const label =
    field.type === 'quantity' ? `${field.label} (${getQuantityUnitLabel(value, field.quantityKind)})` : field.label;
  const labelId = `${field.key}-label`;
  const restartId = `${field.key}-restart`;
  const helpId = `${field.key}-help`;
  const errorId = `${field.key}-error`;
  const describedBy = [helpId, field.requiresRestart && restartId, error && !isHeap && errorId]
    .filter(Boolean)
    .join(' ');
  const customHeapDescribedBy = [helpId, restartId, error && errorId].filter(Boolean).join(' ');
  const onBlur = () => setTouched({ ...touched, [field.key]: true });
  // Configuration keys contain dots; keep literal keys instead of Formik object paths.
  const setValue = (nextValue: string) =>
    setValues((currentValues) => ({ ...currentValues, [field.key]: nextValue }), submitCount > 0);

  const removeOverride = () =>
    setValues((currentValues) => {
      const nextValues = { ...currentValues };
      delete nextValues[field.key];

      return nextValues;
    }, submitCount > 0);

  const renderOptions = () => {
    if (field.type === 'quantity') {
      return (
        <ConfigurationQuantityInput
          id={field.key}
          label={field.label}
          kind={field.quantityKind}
          value={value}
          disabled={disabled}
          labelledBy={labelId}
          describedBy={describedBy}
          invalid={!!error}
          onChange={setValue}
          onBlur={onBlur}
        />
      );
    }

    if (field.type === 'roles') {
      return (
        <Select
          inputId={field.key}
          name={field.key}
          aria-label={field.label}
          inputProps={{ 'aria-describedby': describedBy, 'aria-invalid': !!error }}
          multi
          options={field.options}
          placeholder="No override set"
          value={value}
          onChange={setValue}
          onBlur={onBlur}
          disabled={disabled}
        />
      );
    }

    if (isHeap) {
      return (
        <>
          <option value="" disabled>
            No override set
          </option>
          <option value="automatic" disabled>
            Automatic (50% of available RAM; not available yet)
          </option>
          <option value="custom">Custom size</option>
        </>
      );
    }

    return undefined;
  };

  return (
    <>
      <Input
        id={field.key}
        name={field.key}
        label={
          <FieldLabel>
            <span id={labelId}>{label}</span>
            {field.requiresRestart && (
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
        aria-invalid={!!error && !isHeap}
        {...getControlProps(field.type, value)}
        disabled={disabled}
        placeholder="No override set"
        onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
          if (isHeap && event.target.value === 'custom') return setValue('g');

          return setValue(event.target.value);
        }}
        onBlur={onBlur}
        error={
          error && !isHeap ? (
            <span id={errorId} role="alert">
              {error}
            </span>
          ) : undefined
        }
        help={
          <span id={helpId}>
            {field.description}
            <FieldDetail>
              <code>{field.key}</code>
            </FieldDetail>
            {inheritedValue !== undefined && (
              <FieldDetail>
                {usesGlobalValue ? 'Using global value: ' : 'Global value: '}
                <code>{inheritedValue}</code>
                {!usesGlobalValue && (
                  <>
                    {' '}
                    <Button bsSize="xsmall" disabled={disabled || !canDelete} onClick={removeOverride}>
                      Use global value
                    </Button>
                  </>
                )}
              </FieldDetail>
            )}
            {inheritedValue === undefined && Object.hasOwn(values, field.key) && (
              <FieldDetail>
                <Button
                  bsSize="xsmall"
                  aria-label={`Remove override for ${field.label}`}
                  disabled={disabled || !canDelete}
                  onClick={removeOverride}>
                  Remove override
                </Button>
              </FieldDetail>
            )}
          </span>
        }>
        {renderOptions()}
      </Input>
      {isHeap && value !== '' && value !== 'automatic' && (
        <Input
          id={`${field.key}-custom`}
          label={`Custom heap size (${getQuantityUnitLabel(value, 'heap')})`}
          error={
            error ? (
              <span id={errorId} role="alert">
                {error}
              </span>
            ) : undefined
          }
          help="Set the initial and maximum OpenSearch JVM heap to the same size.">
          <ConfigurationQuantityInput
            id={`${field.key}-custom`}
            label="Custom heap size"
            kind="heap"
            value={value}
            disabled={disabled}
            describedBy={customHeapDescribedBy}
            invalid={!!error}
            onChange={setValue}
            onBlur={onBlur}
          />
        </Input>
      )}
    </>
  );
};

export default ConfigurationFieldInput;
