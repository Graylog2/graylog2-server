// @flow strict
import * as React from 'react';

import User from 'logic/users/User';

import SectionComponent from '../SectionComponent';
import ReadOnlyFormField from '../form/ReadOnlyFormField';

type Props = {
  user: User,
};

const _sessionTimeout = (sessionTimeout) => {
  if (sessionTimeout) {
    return `${sessionTimeout.value} ${sessionTimeout.unitString}`;
  }

  return 'Sessions do not timeout';
};

const SettingsSection = ({
  user: {
    timezone,
    sessionTimeout,
  },
}: Props) => (
  <SectionComponent title="Settings">
    <ReadOnlyFormField label="Sessions Timeout" value={_sessionTimeout(sessionTimeout)} />
    <ReadOnlyFormField label="Timezone" value={timezone} />
  </SectionComponent>
);

export default SettingsSection;
