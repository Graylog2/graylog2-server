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
import { useCallback } from 'react';

import { Input } from 'components/bootstrap';
import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';

type Props = FieldComponentProps & {
  type: 'numeric',
};

const createEvent = (name: string, value: number) => ({ target: { name, value } }) as React.ChangeEvent<any>;

const InputField = ({ type, onChange, value, error, name, title, field }: Props) => {
  const _onChange = useCallback((e: React.ChangeEvent<any>) => (type === 'numeric'
    ? onChange(createEvent(e.target.name, Number.parseFloat(e.target.value)))
    : onChange(e)), [onChange, type]);

  const step = 'step' in field ? field.step : undefined;

  return (
    <Input id={`${name}-input`}
           bsSize="small"
           type={type}
           name={name}
           onChange={_onChange}
           value={value ?? ''}
           label={title}
           error={error}
           placeholder={field.description}
           step={step}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
  );
};

export default InputField;
