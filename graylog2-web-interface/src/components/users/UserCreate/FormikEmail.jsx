// @flow strict
import * as React from 'react';

import FormField from '../form/FormField';

const FormikEmail = () => (
  <FormField label="E-Mail Address"
             name="email"
             type="email"
             required
             help="Give the contact email address." />
);

export default FormikEmail;
