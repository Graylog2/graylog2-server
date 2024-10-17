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
      const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
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
                     key={optionValue}
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
