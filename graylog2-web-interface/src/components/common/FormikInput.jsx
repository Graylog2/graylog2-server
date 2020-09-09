// @flow strict
import * as React from 'react';
import { Field } from 'formik';
import styled, { type StyledComponent, css } from 'styled-components';

import { Input } from 'components/bootstrap';
import type { ThemeInterface } from 'theme';

const ErrorMessage: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
  width: 100%;
  margin-top: 3px;
  color: ${theme.colors.variant.danger};
`);

type Props = {
  label: string,
  name: string,
  type?: string,
  help?: string,
  labelClassName?: string,
  wrapperClassName?: string,
  validate?: (string) => ?string,
};

const checkboxProps = (value) => {
  return { checked: value ?? false };
};

const inputProps = (value) => {
  return { value: value ?? '' };
};

const FormikInput = ({ label, name, type, help, validate, ...rest }: Props) => (
  <Field name={name} validate={validate}>
    {({ field: { value, onChange }, meta: { error } }) => {
      const typeSepcificProps = type === 'checkbox' ? checkboxProps(value) : inputProps(value);

      return (
        <Input {...rest}
               {...typeSepcificProps}
               bsStyle={error ? 'error' : undefined}
               help={error ?? help}
               id={name}
               label={label}
               name={name}
               onChange={onChange}
               type={type}>
          {error && <ErrorMessage>{error}</ErrorMessage>}
        </Input>
      );
    }}
  </Field>
);

FormikInput.ErrorMessage = ErrorMessage;

FormikInput.defaultProps = {
  help: undefined,
  labelClassName: undefined,
  type: 'text',
  validate: () => {},
  wrapperClassName: undefined,
};

export default FormikInput;
