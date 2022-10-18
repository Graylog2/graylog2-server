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
import { useCallback, useEffect, useMemo } from 'react';
import { isFunction } from 'lodash';

import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';

import type { FieldComponentProps } from '../VisualizationConfigurationOptions';

const makeOptions = (options: ReadonlyArray<string | [string, any]>):
  [Array<{ key: string, value: string}>, Set<string>] => {
  const optionsSet = new Set<string>();
  const mappedOptions = options.map((option) => {
    if (typeof option === 'string') {
      optionsSet.add(option);

      return { key: option, value: option };
    }

    const [key, value] = option;
    optionsSet.add(value);

    return { key, value };
  });

  return [mappedOptions, optionsSet];
};

const createEvent = (name: string, value: any) => ({ target: { name, value } }) as React.ChangeEvent<any>;

const MultiSelectField = ({ name, field, title, error, value, onChange, values }: FieldComponentProps) => {
  if (field.type !== 'multi-select') {
    throw new Error('Invalid field type passed!');
  }

  const [selectOption, optionsSet] = useMemo(() => {
    if (isFunction(field.options)) return makeOptions(field.options({ formValues: values }));

    return makeOptions(field.options);
  }, [values, field]);

  const onSelect = useCallback((newValue: string) => onChange(createEvent(name, newValue === '' ? [] : newValue.split(','))), [name, onChange]);
  const selectedValue = useMemo(() => value.join(','), [value]);

  useEffect(() => {
    const checkedValue = value.filter((option) => optionsSet.has(option)).join(',');

    if (selectedValue !== checkedValue) {
      onSelect(checkedValue);
    }
  }, [optionsSet, value, onSelect, selectedValue]);

  return (
    <Input id={`${name}-select`}
           label={title}
           error={error}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9">
      <Select options={selectOption}
              name={name}
              value={selectedValue}
              multi
              menuPortalTarget={document.body}
              onChange={onSelect}
              inputProps={{ 'aria-label': `Select ${field.title}` }}
              displayKey="key"
              inputId="multi-select-visualization" />
    </Input>
  );
};

export default MultiSelectField;
