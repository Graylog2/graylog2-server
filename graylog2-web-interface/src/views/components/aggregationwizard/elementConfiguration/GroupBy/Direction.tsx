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

  div:first-child {
    margin-right: 5px;
  }
`;

type Props = {
  index: number,
};

const Direction = ({ index }: Props) => (
  <Field name={`groupBy.groupings.${index}.direction`}>
    {({ field: { name, value, onChange, onBlur }, meta: { error } }) => (
      <Input id="group-by-direction"
             label="Direction"
             error={error}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9">
        <DirectionOptions>
          <Input defaultChecked={value === 'row'}
                 formGroupClassName=""
                 id={name}
                 label="Row"
                 onBlur={onBlur}
                 onChange={onChange}
                 type="radio"
                 value="row" />
          <Input defaultChecked={value === 'column'}
                 formGroupClassName=""
                 id={name}
                 label="Column"
                 onBlur={onBlur}
                 onChange={onChange}
                 type="radio"
                 value="column" />
        </DirectionOptions>
      </Input>
    )}
  </Field>
);

export default Direction;
