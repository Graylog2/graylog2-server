import * as React from 'react';

import { Input } from 'components/bootstrap';
import { Checkbox } from 'components/graylog';
import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';

const BooleanField = ({ field, name, onChange, error, title, value }: FieldComponentProps) => {
  return (
    <Input id={`${name}-input`}
           label={title}
           error={error}
           help={field.description}
           labelClassName="col-sm-11"
           wrapperClassName="col-sm-1">
      <Checkbox id={`${name}-input`} name={name} onChange={onChange} checked={value} />
    </Input>

  );
};

export default BooleanField;
