// @flow strict
import * as React from 'react';

import FormRow from './FormRow';

type Props = {
  label: string,
  value: string,
};

const FormFieldRead = ({ label, value }: Props) => (
  <FormRow label={<b>{label}</b>}>
    {value}
  </FormRow>

);

export default FormFieldRead;
