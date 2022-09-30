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
import { useMemo } from 'react';
import { isFunction } from 'lodash';

import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';

import type { FieldComponentProps } from '../VisualizationConfigurationOptions';

const makeOptions = (options: ReadonlyArray<string | [string, any]>) => {
  return options.map((option) => {
    if (typeof option === 'string') {
      return { key: option, value: option };
    }

    const [key, value] = option;

    return { key, value };
  });
};

const createEvent = (name: string, value: any) => ({ target: { name, value } }) as React.ChangeEvent<any>;

const MultiSelectField = ({ name, field, title, error, value, onChange, values }: FieldComponentProps) => {
  if (field.type !== 'multi-select') {
    throw new Error('Invalid field type passed!');
  }

  const selectOption = useMemo(() => {
    if (isFunction(field.options)) return makeOptions(field.options({ formValues: values }));

    return makeOptions(field.options);
  }, [values, field]);

  return (
    <Input id={`${name}-select`}
           label={title}
           error={error}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9">
      <Select options={selectOption}
              aria-label={`Select ${field.title}`}
              name={name}
              value={value.join(',')}
              multi
              menuPortalTarget={document.body}
              onChange={(newValue: string) => onChange(createEvent(name, newValue === '' ? [] : newValue.split(',')))}
              inputProps={{ 'aria-label': '' }}
              displayKey="key"
              inputId="multi-select-visualization" />
    </Input>
  );
};

export default MultiSelectField;
