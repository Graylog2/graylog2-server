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
  buttonAfter?: React.ReactElement | string,
  field: TextFieldType,
  dirty: boolean,
  onChange: (title: string, value: number) => void,
  title: string,
  typeName: string,
  value?: string,
};

const TextField = ({ field, title, typeName, dirty, onChange, value, autoFocus, buttonAfter }: Props) => {
  const isRequired = !field.is_optional;
  const showReadOnlyEncrypted = field.is_encrypted && !dirty && value !== '';
  const fieldType = (!hasAttribute(field.attributes, 'textarea') && (hasAttribute(field.attributes, 'is_password') || showReadOnlyEncrypted) ? 'password' : 'text');
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
           readOnly={showReadOnlyEncrypted}
           onChange={handleChange}
           buttonAfter={buttonAfter}
           autoFocus={autoFocus} />
  );
};

TextField.propTypes = {
  autoFocus: PropTypes.bool,
  buttonAfter: PropTypes.node,
  dirty: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.string,
};

TextField.defaultProps = {
  autoFocus: false,
  buttonAfter: undefined,
  dirty: false,
  value: '',
};

export default TextField;
