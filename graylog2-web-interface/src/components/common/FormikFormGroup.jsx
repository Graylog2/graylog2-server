// @flow strict
import * as React from 'react';

import FormikInput from './FormikInput';

type Props = {
  label: string,
  name: string,
  type?: string,
  help?: string,
  validate?: (string) => ?string,
};

const FormikFormGroup = ({ label, name, type, help, validate, ...rest }: Props) => (
  <FormikInput {...rest}
               help={help}
               label={label}
               id={name}
               name={name}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9"
               type={type} />
);

FormikFormGroup.defaultProps = {
  type: 'text',
  help: undefined,
  validate: () => {},
};

export default FormikFormGroup;
