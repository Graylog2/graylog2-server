/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { UserJSON } from 'logic/users/User';

import UserTimezoneTimestamp from './UserTimezoneTimestamp';

jest.mock('util/AppConfig');

const createCurrentUserWithTz = (tz: string): UserJSON => ({
  timezone: tz,
} as UserJSON);

describe('UserTimezoneTimestamp', () => {
  const WithTimezone = ({ children, tz }: { children: React.ReactNode, tz: string }) => (
    <CurrentUserContext.Provider value={createCurrentUserWithTz(tz)}>
      {children}
    </CurrentUserContext.Provider>
  );

  it('should use current user\'s timezone to render timestamp', async () => {
    render((
      <WithTimezone tz="America/New_York">
        <UserTimezoneTimestamp dateTime="2020-11-30T11:09:00.950Z" />
      </WithTimezone>
    ));

    await screen.findByText('2020-11-30 06:09:00.950 -05:00');
  });

  it('should default to system timezone to render timestamp', async () => {
    asMock(AppConfig.rootTimeZone).mockReturnValue('Asia/Tokyo');

    render((
      <WithTimezone tz={undefined}>
        <UserTimezoneTimestamp dateTime="2020-11-30T11:09:00.950Z" />
      </WithTimezone>
    ));

    await screen.findByText('2020-11-30 20:09:00.950 +09:00');
  });
});
