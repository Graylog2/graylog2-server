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
import { useContext } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import moment from 'moment-timezone';
import { alice } from 'fixtures/users';

import TimeLocalizeProvider, { ACCEPTED_FORMATS } from 'contexts/TimeLocalizeProvider';
import TimeLocalizeContext from 'contexts/TimeLocalizeContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import User from 'logic/users/User';

const TestTimestamp = ({ timestamp }: { timestamp: string }) => {
  const { localizeTime } = useContext(TimeLocalizeContext);

  return (<div>{localizeTime(timestamp)}</div>);
};

const SUT = ({ timestamp, user }: { timestamp: string, user: User }) => (
  <CurrentUserContext.Provider value={user.toJSON()}>
    <TimeLocalizeProvider>
      <TestTimestamp timestamp={timestamp} />
    </TimeLocalizeProvider>,
  </CurrentUserContext.Provider>
);

describe('TimeLocalizeProvider', () => {
  it('should provide a function which converts a date time string to local time of user', () => {
    const user = alice.toBuilder().timezone('Asia/Tokyo').build();

    render(<SUT timestamp="2021-03-27T14:32:31.894Z" user={user} />);

    expect(screen.getByText('2021-03-27 23:32:31.894 +09:00')).toBeInTheDocument();
  });

  it('should provide a function which converts a date time string to local time of user without timezone', () => {
    const user = alice.toBuilder().timezone(undefined).build();

    render(<SUT timestamp="2021-03-27T14:32:31.894Z" user={user} />);

    const browserTz = moment.tz.guess();
    const result = moment.tz('2021-03-27T14:32:31.894Z', ACCEPTED_FORMATS.ISO_8601, true, browserTz)
      .format(ACCEPTED_FORMATS.TIMESTAMP_TZ);

    expect(screen.getByText(result)).toBeInTheDocument();
  });
});
