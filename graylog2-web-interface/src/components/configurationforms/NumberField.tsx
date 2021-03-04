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

import Input from 'components/bootstrap/Input';
import { getValueFromInput } from 'util/FormsUtils';

import type { NumberField as NumberFieldType } from './types';
import FieldHelpers from './FieldHelpers';

type Props = {
  autoFocus?: boolean,
  field: NumberFieldType,
  onChange: (title: string, value: number) => void,
  title: string,
  typeName: string,
  value: number,
};

const NumberField = ({ autoFocus, field, onChange, title, typeName, value }: Props) => {
  const _getDefaultValidationSpecs = () => {
    return { min: Number.MIN_SAFE_INTEGER, max: Number.MAX_SAFE_INTEGER };
  };

  const _mapValidationAttribute = (attribute) => {
    const { min, max } = _getDefaultValidationSpecs();

    switch (attribute.toLocaleUpperCase()) {
      case 'ONLY_NEGATIVE': return { min: min, max: -1 };
      case 'ONLY_POSITIVE': return { min: 0, max: max };
      case 'IS_PORT_NUMBER': return { min: 0, max: 65535 };
      default: return { min, max };
    }
  };

  const validationSpec = () => {
    const validationAttributes = field.attributes.map(_mapValidationAttribute);

    if (validationAttributes.length > 0) {
      // The server may return more than one validation attribute, but it doesn't make sense to use more
      // than one validation for a number field, so we return the first one
      return validationAttributes[0];
    }

    return _getDefaultValidationSpecs();
  };

  const handleChange = ({ target }) => {
    const numericValue = getValueFromInput(target);

    onChange(title, numericValue);
  };

  const isRequired = !field.is_optional;
  const validationSpecs = validationSpec();

  const label = <span>{field.human_name} {FieldHelpers.optionalMarker(field)}</span>;

  return (
    <Input id={`${typeName}-${title}`}
           label={label}
           type="number"
           name={`configuration[${title}]`}
           className="input-xlarge validatable"
           required={isRequired}
           onChange={handleChange}
           value={value}
           help={field.description}
           {...validationSpecs}
           autoFocus={autoFocus} />
  );
};

NumberField.propTypes = {
  autoFocus: PropTypes.bool,
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  typeName: PropTypes.string.isRequired,
  value: PropTypes.number,
};

NumberField.defaultProps = {
  autoFocus: false,
  value: 0,
};

export default NumberField;
