import * as React from 'react';
import styled from 'styled-components';
import { Field } from 'formik';

import { Input } from 'components/bootstrap';

const DirectionOptions = styled.div`
  display: flex;
  
  .radio {
    padding-top: 6px;
  }
  
  div:first-child {
    margin-right: 5px;
  }
`;

type Props = {
  name: string,
  onChange: (type: string) => void,
  options: Array<{ label: string, value: string }>,
}

const WidgetModeConfiguration = ({ name, onChange: onChangeProp, options }: Props) => (
  <Field name={name}>
    {({ field: { value, onChange, onBlur }, meta: { error } }) => {
      const handleChange = (event: React.ChangeEvent<Input>) => {
        onChangeProp(event.target.value);
        onChange(event);
      };

      return (
        <Input id="widget-type-configuration"
               label="Type"
               error={error}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <DirectionOptions>
            {options.map(({ value: optionValue, label }) => (
              <Input checked={value === optionValue}
                     formGroupClassName=""
                     id={name}
                     label={label}
                     onBlur={onBlur}
                     onChange={handleChange}
                     type="radio"
                     value={optionValue} />
            ))}
          </DirectionOptions>
        </Input>
      );
    }}
  </Field>
);

export default WidgetModeConfiguration;
