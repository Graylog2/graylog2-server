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
import { Field } from 'formik';

import { FormikInput, Select } from 'components/common';
import { Input } from 'components/bootstrap';

import { useFieldError } from './fieldError';

import { enumLabel, fieldKind, isNullable, nonNullSchema } from '../schema';
import type { RuntimeTunableFieldProps } from '../types';

const emptyToNull = (value: unknown) => (value === '' || value === undefined ? null : value);

const ScalarField = ({ name, schema, label, help, disabled = false }: RuntimeTunableFieldProps) => {
  const error = useFieldError(name);
  const kind = fieldKind(schema);
  const actual = nonNullSchema(schema);
  const nullable = isNullable(schema);

  if (kind === 'enum') {
    const options = (actual.enum ?? [])
      .filter((option) => option !== null)
      .map((option) => ({ label: enumLabel(actual, option), value: option }));

    return (
      <Field name={name}>
        {({ field: { value }, form: { setFieldValue } }) => (
          <Input id={name} label={label} help={help} error={error}>
            <Select
              inputId={name}
              options={options}
              value={value}
              clearable={nullable}
              disabled={disabled}
              onChange={(newValue: string) => setFieldValue(name, nullable ? emptyToNull(newValue) : newValue)}
            />
          </Input>
        )}
      </Field>
    );
  }

  if (kind === 'boolean') {
    return <FormikInput type="checkbox" id={name} name={name} label={label} help={help} disabled={disabled} />;
  }

  const numeric = kind === 'integer' || kind === 'number';

  if (nullable && kind === 'string') {
    return (
      <Field name={name}>
        {({ field: { value }, form: { setFieldValue } }) => (
          <Input
            id={name}
            type="text"
            label={label}
            help={help}
            error={error}
            disabled={disabled}
            maxLength={actual.maxLength}
            value={value ?? ''}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => setFieldValue(name, emptyToNull(event.target.value))}
          />
        )}
      </Field>
    );
  }

  return (
    <FormikInput
      type={numeric ? 'number' : 'text'}
      id={name}
      name={name}
      label={label}
      help={help}
      disabled={disabled}
      min={schema.minimum}
      max={schema.maximum}
      maxLength={schema.maxLength}
    />
  );
};

export default ScalarField;
