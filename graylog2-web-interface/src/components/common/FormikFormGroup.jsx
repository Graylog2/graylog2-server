// @flow strict
import * as React from 'react';

import { Input } from 'components/bootstrap';

import FormikInput from './FormikInput';

type Props = {
  label: string,
  name: string,
  onChange?: (SyntheticInputEvent<Input>) => void,
  labelClassName?: string,
  wrapperClassName?: string,
};

/** Displays the FormikInput with a specific layout */
const FormikFormGroup = ({ labelClassName, wrapperClassName, label, name, onChange, ...rest }: Props) => (
  <FormikInput {...rest}
               label={label}
               id={name}
               onChange={onChange}
               name={name}
               labelClassName={labelClassName}
               wrapperClassName={wrapperClassName} />
);

FormikFormGroup.defaultProps = {
  onChange: undefined,
  labelClassName: 'col-sm-3',
  wrapperClassName: 'col-sm-9',
};

export default FormikFormGroup;
