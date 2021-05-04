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

import { DropdownField as DropdownFieldType } from 'components/configurationforms/types';
import { Input } from 'components/bootstrap';
import { optionalMarker } from 'components/configurationforms/FieldHelpers';

type Props = {
  autoFocus?: boolean,
  field: DropdownFieldType,
  onChange: (title: string, value: string) => void,
  title: string,
  typeName: string,
  value: string,
  addPlaceholder: boolean,
};

const DropdownField = ({ autoFocus, field, onChange, title, typeName, value, addPlaceholder }: Props) => {
  const formatOption = (key, displayValue, disabled = false) => {
    return (
      <option key={`${typeName}-${title}-${key}`} value={key} id={key} disabled={disabled}>{displayValue}</option>
    );
  };

  const handleChange = (event) => {
    onChange(title, event.target.value);
  };

  const options = Object.entries(field.additional_info.values).map(([k, v]) => formatOption(k, v));

  if (addPlaceholder) {
    options.unshift(formatOption('', `Select ${field.human_name || title}`, true));
  }

  const label = <>{field.human_name} {optionalMarker(field)}</>;

  return (
    <Input id={`${typeName}-${title}`}
           name={`configuration[${title}]`}
           label={label}
           type="select"
           value={value}
           help={field.description}
           onChange={handleChange}
           autoFocus={autoFocus}
           required={!field.is_optional}>
      {options}
    </Input>
  );
};

DropdownField.propTypes = {
  autoFocus: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.string,
  addPlaceholder: PropTypes.bool,
};

DropdownField.defaultProps = {
  autoFocus: false,
  addPlaceholder: false,
  value: '',
};

export default DropdownField;
