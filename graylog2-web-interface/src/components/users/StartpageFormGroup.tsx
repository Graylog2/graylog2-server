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
import { Field } from 'formik';
import { upperFirst } from 'lodash';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';

const Container = styled.div`
  display:flex;
  align-items: center;
  padding-top: 6px;
`;

const ResetBtn = styled(Button)`
  margin-left: 5px;
`;

const StartpageFormGroup = () => (
  <Field name="startpage">
    {({ field: { name, value, onChange } }) => {
      const valueTxt = value?.type
        ? `${upperFirst(value.type)} ${value.id}`
        : 'No Startpage set';
      const resetBtn = value?.type
        ? (
          <ResetBtn bsSize="xs"
                    onClick={() => onChange({ target: { name, value: {} } })}>
            Reset
          </ResetBtn>
        )
        : null;

      return (
        <>
          <Input id="startpage"
                 label="Startpage"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Container>{valueTxt}{resetBtn}</Container>
          </Input>
        </>
      );
    }}
  </Field>
);

export default StartpageFormGroup;
