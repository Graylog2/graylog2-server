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
import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';
import { hasAttribute, optionalMarker } from 'components/configurationforms/FieldHelpers';
import { getValueFromInput } from 'util/FormsUtils';

import type { TextField as TextFieldType } from './types';

type Props = {
  autoFocus: boolean,
  field: TextFieldType,
  onChange: (title: string, value: number) => void,
  title: string,
  typeName: string,
  value?: string,
};

const TextField = ({ field, title, typeName, onChange, value, autoFocus }: Props) => {
  const isRequired = !field.is_optional;
  const fieldType = (!hasAttribute(field.attributes, 'textarea') && hasAttribute(field.attributes, 'is_password') ? 'password' : 'text');
  const fieldId = `${typeName}-${title}`;

  const labelContent = <>{field.human_name} {optionalMarker(field)}</>;

  const handleChange = ({ target }) => {
    const inputValue = getValueFromInput(target);

    onChange(title, inputValue);
  };

  if (hasAttribute(field.attributes, 'textarea')) {
    return (
      <Input id={fieldId}
             type="textarea"
             rows={10}
             label={labelContent}
             name={`configuration[${title}]`}
             required={isRequired}
             help={field.description}
             value={value || ''}
             onChange={handleChange}
             autoFocus={autoFocus} />
    );
  }

  return (
    <Input id={fieldId}
           type={fieldType}
           name={`configuration[${title}]`}
           label={labelContent}
           required={isRequired}
           help={field.description}
           value={value || ''}
           onChange={handleChange}
           autoFocus={autoFocus} />
  );
};

TextField.propTypes = {
  autoFocus: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.string,
};

TextField.defaultProps = {
  autoFocus: false,
  value: '',
};

export default TextField;
