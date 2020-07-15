// @flow strict
import * as React from 'react';

import FormField from '../form/FormField';

const FormikFullName = () => (
  <FormField label="Full Name"
             name="full_name"
             required
             help="Give a descriptive name for this account, e.g. the full name." />
);

export default FormikFullName;
