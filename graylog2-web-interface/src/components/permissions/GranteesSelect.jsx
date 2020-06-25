// @flow strict
import * as React from 'react';

import type { AvailableGrantees } from 'logic/permissions/EntityShareState';

type Props = {
  availableGrantees: AvailableGrantees,
};

const GranteeSelect = ({ availableGrantees }: Props) => {
  return (
    <>
      {availableGrantees.map((grantee) => <p key={grantee.title}>{grantee.title}</p>)}
    </>
  );
};

export default GranteeSelect;
