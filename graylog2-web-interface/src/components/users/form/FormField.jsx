// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import { Input } from 'components/bootstrap';

import FormRow from './FormRow';

type Props = {
  label: string,
  name: string,
  type?: string,
};

const FormField = ({ label, name, type, ...rest }: Props) => (
  <FormRow label={<label htmlFor={name}>{label}</label>}>
    <Field name={name}>
      {({ field: { value, onChange } }) => (
        <Input {...rest}
               id={name}
               name={name}
               onChange={onChange}
               type={type}
               value={value} />
      )}
    </Field>
  </FormRow>

);

FormField.defaultProps = {
  type: 'text',
};

export default FormField;
