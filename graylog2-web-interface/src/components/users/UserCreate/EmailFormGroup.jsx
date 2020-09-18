// @flow strict
import * as React from 'react';

import FormsUtils from 'util/FormsUtils';
import { FormikFormGroup } from 'components/common';

function validateEmail(value) {
  let error;

  if (!value) {
    error = 'Required';
  }

  return error;
}

const EmailFormGroup = () => (
  <FormikFormGroup label="E-Mail Address"
                   name="email"
                   maxLength={254}
                   type="email"
                   required
                   validate={validateEmail}
                   help="Give the contact email address." />
);

export default EmailFormGroup;
