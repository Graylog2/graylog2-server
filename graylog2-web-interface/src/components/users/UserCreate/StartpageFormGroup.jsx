// @flow strict
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
