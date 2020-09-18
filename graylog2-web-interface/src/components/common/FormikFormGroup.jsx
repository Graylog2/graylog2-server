// @flow strict
import * as React from 'react';

import FormikInput from './FormikInput';

type Props = {
  label: string,
  name: string,
};

/** Displays the FormikInput with a specific layout */
const FormikFormGroup = ({ label, name, ...rest }: Props) => (
  <FormikInput {...rest}
               label={label}
               id={name}
               name={name}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
);
export default FormikFormGroup;
