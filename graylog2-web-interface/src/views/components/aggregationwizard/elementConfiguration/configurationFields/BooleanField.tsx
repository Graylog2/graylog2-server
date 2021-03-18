import * as React from 'react';

import { Input } from 'components/bootstrap';
import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';

const BooleanField = ({ field, name, onChange, error, title, value }: FieldComponentProps) => {
  return (
    <Input id={`${name}-input`}
           type="checkbox"
           name={name}
           error={error}
           label={title}
           defaultChecked={value}
           onChange={onChange}
           help={field.description} />

  );
};

export default BooleanField;
