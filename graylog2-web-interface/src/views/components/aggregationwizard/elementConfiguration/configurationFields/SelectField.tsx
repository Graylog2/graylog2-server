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

import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';

const makeOptions = (options: ReadonlyArray<string | [string, any]>) => {
  return options.map((option) => {
    if (typeof option === 'string') {
      return { label: option, value: option };
    }

    const [label, value] = option;

    return { label, value };
  });
};

const createEvent = (name: string, value: any) => ({ target: { name, value } }) as React.ChangeEvent<any>;

const SelectField = ({ name, field, title, error, value, onChange }: FieldComponentProps) => {
  if (field.type !== 'select') {
    throw new Error('Invalid field type passed!');
  }

  return (
    <Input id={`${name}-select`}
           label={title}
           error={error}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9">
      <Select options={makeOptions(field.options)}
              aria-label={`Select ${field.title}`}
              clearable={!field.required}
              name={name}
              value={value}
              size="small"
              onChange={(newValue) => onChange(createEvent(name, newValue))} />
    </Input>
  );
};

export default SelectField;
