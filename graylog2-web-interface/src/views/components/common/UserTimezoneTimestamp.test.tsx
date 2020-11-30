import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import UserTimezoneTimestamp from './UserTimezoneTimestamp';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { UserJSON } from 'logic/users/User';

const createCurrentUserWithTz = (tz: string): UserJSON => ({
  timezone: tz,
} as UserJSON);

describe('UserTimezoneTimestamp', () => {
  const WithTimezone = ({ children, tz }) => (
    <CurrentUserContext.Provider value={createCurrentUserWithTz(tz)}>
      {children}
    </CurrentUserContext.Provider>
  );

  it('should use current user\'s timezone to render timestamp', async () => {
    render((
      <WithTimezone tz="America/New_York">
        <UserTimezoneTimestamp dateTime="2020-11-30T11:09:00.950Z" />
      </WithTimezone>
    ))

    await screen.findByText('2020-11-30 06:09:00.950 -05:00');
  });

  it('should default to system timezone to render timestamp', async () => {
    render((
      <WithTimezone tz={undefined}>
        <UserTimezoneTimestamp dateTime="2020-11-30T11:09:00.950Z" />
      </WithTimezone>
    ))

    await screen.findByText('2020-11-30 12:09:00.950 +01:00');
  });
});
