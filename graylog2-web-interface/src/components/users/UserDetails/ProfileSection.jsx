// @flow strict
import * as React from 'react';

import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import ShowConfigValue from '../ShowConfigValue';
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
    <ShowConfigValue label="Username" value={username} />
    <ShowConfigValue label="Full name" value={fullName} />
    <ShowConfigValue label="E-Mail Address" value={email} />
    <ShowConfigValue label="Client Address" value={clientAddress} />
    <ShowConfigValue label="Last Activity" value={lastActivity} />
    <ShowConfigValue label="Logged In" value={<LoggedInIcon active={sessionActive} />} />
  </SectionComponent>
);

export default ProfileSection;
