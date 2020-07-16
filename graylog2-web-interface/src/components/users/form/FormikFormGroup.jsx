// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import { Input } from 'components/bootstrap';

import FieldError from './FieldError';

type Props = {
  label: string,
  name: string,
  type?: string,
  help?: string,
  validate?: (string) => ?string,
};

const FormField = ({ label, name, type, help, validate, ...rest }: Props) => (
  <Field name={name} validate={validate}>
    {({ field: { value, onChange }, meta: { error } }) => (
      <Input {...rest}
             help={error ?? help}
             label={label}
             id={name}
             bsStyle={error ? 'error' : undefined}
             name={name}
             onChange={onChange}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             type={type}
             value={value ?? ''}>
        {error && <FieldError>{error}</FieldError>}
      </Input>
    )}
  </Field>

);

FormField.defaultProps = {
  type: 'text',
  help: undefined,
  validate: () => {},
};

export default FormField;
