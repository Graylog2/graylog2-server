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
              clearable={!field.required}
              name={name}
              value={value}
              onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
    </Input>
  );
};

export default SelectField;
