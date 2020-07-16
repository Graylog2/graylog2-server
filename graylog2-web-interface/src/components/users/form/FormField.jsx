// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import { Input } from 'components/bootstrap';

type Props = {
  label: string,
  name: string,
  type?: string,
};

const FormField = ({ label, name, type, ...rest }: Props) => (
  <Field name={name}>
    {({ field: { value, onChange } }) => (
      <Input {...rest}
             label={label}
             id={name}
             name={name}
             onChange={onChange}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             type={type}
             value={value} />
    )}
  </Field>

);

FormField.defaultProps = {
  type: 'text',
};

export default FormField;
