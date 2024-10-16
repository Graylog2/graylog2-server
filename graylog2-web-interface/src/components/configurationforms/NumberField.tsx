import React from 'react';

import Input from 'components/bootstrap/Input';
import { getValueFromInput } from 'util/FormsUtils';
import { optionalMarker } from 'components/configurationforms/FieldHelpers';

import type { NumberField as NumberFieldType } from './types';

type Props = {
  autoFocus?: boolean,
  field: NumberFieldType,
  onChange: (title: string, value: number, dirty?: boolean) => void,
  title: string,
  typeName: string,
  value?: number
};

const NumberField = ({ autoFocus = false, field, onChange, title, typeName, value = 0 }: Props) => {
  const _getDefaultValidationSpecs = () => ({ min: Number.MIN_SAFE_INTEGER, max: Number.MAX_SAFE_INTEGER });

  const _mapValidationAttribute = (attribute) => {
    const { min, max } = _getDefaultValidationSpecs();

    switch (attribute.toUpperCase()) {
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

  const label = <>{field.human_name} {optionalMarker(field)}</>;

  return (
    <Input id={`${typeName}-${title}`}
           label={label}
           type="number"
           name={`configuration[${title}]`}
           required={isRequired}
           onChange={handleChange}
           value={value || 0}
           help={field.description}
           {...validationSpecs}
           autoFocus={autoFocus} />
  );
};

export default NumberField;
