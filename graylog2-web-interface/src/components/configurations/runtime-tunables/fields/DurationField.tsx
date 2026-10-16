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

import { TimeUnitInput } from 'components/common';

import { useFieldError } from './fieldError';

import type { DurationUnit, RuntimeTunableFieldProps } from '../types';
import { DEFAULT_UNITS, formatDuration, splitDuration } from '../duration';

const DurationField = ({ name, schema, label, help, disabled = false }: RuntimeTunableFieldProps) => {
  const error = useFieldError(name);
  const units = schema['x-graylog-units'] ?? DEFAULT_UNITS;

  return (
    <Field name={name}>
      {({ field: { value }, form: { setFieldValue } }) => {
        const current = splitDuration(value, units) ?? { value: 1, unit: units[0] };

        return (
          <TimeUnitInput
            label={label}
            name={name}
            value={current.value}
            unit={current.unit.toUpperCase()}
            units={units.map((unit) => unit.toUpperCase())}
            update={(newValue: number, newUnit: string) =>
              setFieldValue(name, formatDuration(newValue, newUnit.toLowerCase() as DurationUnit))
            }
            help={error ? <span className="text-danger">{error}</span> : help}
            disabled={disabled}
            hideCheckbox
            required
          />
        );
      }}
    </Field>
  );
};

export default DurationField;
