// @flow strict
import * as React from 'react';

import { FormikFormGroup } from 'components/common';

const FullNameFormGroup = () => (
  <FormikFormGroup label="Full Name"
                   name="full_name"
                   maxLength={200}
                   required
                   help="Give a descriptive name for this account, e.g. the full name." />
);

export default FullNameFormGroup;
