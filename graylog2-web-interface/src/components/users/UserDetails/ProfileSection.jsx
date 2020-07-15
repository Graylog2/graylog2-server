// @flow strict
import * as React from 'react';

import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import ReadOnlyFormField from '../form/ReadOnlyFormField';
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
    <ReadOnlyFormField label="Username" value={username} />
    <ReadOnlyFormField label="Full name" value={fullName} />
    <ReadOnlyFormField label="E-Mail Address" value={email} />
    <ReadOnlyFormField label="Client Address" value={clientAddress} />
    <ReadOnlyFormField label="Last Activity" value={lastActivity} />
    <ReadOnlyFormField label="Logged In" value={<LoggedInIcon active={sessionActive} />} />
  </SectionComponent>
);

export default ProfileSection;
