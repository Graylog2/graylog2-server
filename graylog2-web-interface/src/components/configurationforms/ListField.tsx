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
import PropTypes from 'prop-types';

import { ListField as ListFieldType } from 'components/configurationforms/types';
import { MultiSelect } from 'components/common';
import { Input } from 'components/bootstrap';
import { optionalMarker } from 'components/configurationforms/FieldHelpers';

type Props = {
  autoFocus?: boolean,
  field: ListFieldType,
  onChange: (title: string, value: Array<string>) => void,
  title: string,
  typeName: string,
  value: Array<string> | string,
};

const ListField = ({ autoFocus, field, onChange, title, typeName, value }: Props) => {
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

  return (
    <Input id={`${typeName}-${title}`}
           label={label}
           help={field.description}>
      <MultiSelect inputId={`${typeName}-${title}`}
                   name={`configuration[${title}]`}
                   required={isRequired}
                   autoFocus={autoFocus}
                   options={formattedOptions}
                   value={value ? (Array.isArray(value) ? value.join(',') : value) : undefined}
                   placeholder={`${allowCreate ? 'Add' : 'Select'} ${field.human_name}`}
                   onChange={handleChange}
                   allowCreate={allowCreate} />
    </Input>
  );
};

ListField.propTypes = {
  autoFocus: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
};

ListField.defaultProps = {
  autoFocus: false,
  value: undefined,
};

export default ListField;
