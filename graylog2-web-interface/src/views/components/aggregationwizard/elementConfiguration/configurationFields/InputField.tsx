import * as React from 'react';
import { useCallback } from 'react';

import { Input } from 'components/bootstrap';
import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';

type Props = FieldComponentProps & {
  type: 'numeric',
};

const createEvent = (name: string, value: number) => ({ target: { name, value } }) as React.ChangeEvent<any>;

const InputField = ({ type, onChange, value, error, name, title, field }: Props) => {
  const _onChange = useCallback((e: React.ChangeEvent<any>) => (type === 'numeric'
    ? onChange(createEvent(e.target.name, Number.parseFloat(e.target.value)))
    : onChange(e)), [onChange, type]);

  return (
    <Input id={`${name}-input`}
           type={type}
           name={name}
           onChange={_onChange}
           value={value}
           label={title}
           error={error}
           placeholder={field.description}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
  );
};

export default InputField;
