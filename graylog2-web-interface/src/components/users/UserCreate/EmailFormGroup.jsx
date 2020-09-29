// @flow strict
import * as React from 'react';

import { FormikFormGroup } from 'components/common';

const EmailFormGroup = () => (
  <FormikFormGroup label="E-Mail Address"
                   name="email"
                   maxLength={254}
                   type="email"
                   required
                   help="Give the contact email address." />
);

export default EmailFormGroup;
