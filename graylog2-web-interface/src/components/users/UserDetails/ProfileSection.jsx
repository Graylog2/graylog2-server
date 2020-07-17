// @flow strict
import * as React from 'react';

import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import ReadOnlyFormGroup from '../form/ReadOnlyFormGroup';
import LoggedInIcon from '../LoggedInIcon';

type Props = {
  user: User,
};

const ProfileSection = ({
  user: {
    username,
    fullName,
    email,
    clientAddress,
    lastActivity,
    sessionActive,
  },
}: Props) => (
  <SectionComponent title="Profile">
    <ReadOnlyFormGroup label="Username" value={username} />
    <ReadOnlyFormGroup label="Full name" value={fullName} />
    <ReadOnlyFormGroup label="E-Mail Address" value={email} />
    <ReadOnlyFormGroup label="Client Address" value={clientAddress} />
    <ReadOnlyFormGroup label="Last Activity" value={lastActivity} />
    <ReadOnlyFormGroup label="Logged In" value={<LoggedInIcon active={sessionActive} />} />
  </SectionComponent>
);

export default ProfileSection;
