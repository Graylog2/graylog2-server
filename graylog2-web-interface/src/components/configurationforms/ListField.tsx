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

import type { ListField as ListFieldType } from 'components/configurationforms/types';
import { MultiSelect } from 'components/common';
import { Input } from 'components/bootstrap';
import { optionalMarker } from 'components/configurationforms/FieldHelpers';

type Props = {
  autoFocus?: boolean,
  field: ListFieldType,
  onChange: (title: string, value: Array<string>, dirty?: boolean) => void,
  title: string,
  typeName: string,
  value?: Array<string> | string
};

const ListField = ({ autoFocus = false, field, onChange, title, typeName, value }: Props) => {
  const handleChange = (nextValue) => {
    const values = (nextValue === '' ? [] : nextValue.split(','));

    onChange(title, values);
  };

  const isRequired = !field.is_optional;
  const allowCreate = field.attributes.includes('allow_create');
  const options = field.additional_info?.values || {};
  const formattedOptions = Object.entries(options)
    .map(([label, optionValue]) => ({ value: optionValue, label: label }));

  const label = <>{field.human_name} {optionalMarker(field)}</>;

  const selectValue = Array.isArray(value) ? value.join(',') : value;

  return (
    <Input id={`${typeName}-${title}`}
           label={label}
           help={field.description}>
      <MultiSelect inputId={`${typeName}-${title}`}
                   name={`configuration[${title}]`}
                   required={isRequired}
                   autoFocus={autoFocus}
                   className="list-field-select"
                   options={formattedOptions}
                   value={selectValue}
                   placeholder={`${allowCreate ? 'Add' : 'Select'} ${field.human_name}`}
                   onChange={handleChange}
                   allowCreate={allowCreate} />
    </Input>
  );
};

export default ListField;
