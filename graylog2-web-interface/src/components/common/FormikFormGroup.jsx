// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import FormikInput from './FormikInput';

type Props = {
  label: string,
  name: string,
  type?: string,
  component?: typeof Field,
};

/** Displays the FormikInput with a specific layout */
const FormikFormGroup = ({ component, label, name, type, ...rest }: Props) => (
  <FormikInput {...rest}
               label={label}
               id={name}
               name={name}
               component={component}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9"
               type={type} />
);

FormikFormGroup.defaultProps = {
  type: 'text',
  component: Field,
};

export default FormikFormGroup;
