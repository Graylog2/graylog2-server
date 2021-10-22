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
import { alice } from 'fixtures/users';

import DateTimeProvider from 'contexts/DateTimeProvider';
import DateTimeContext from 'contexts/DateTimeContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import User from 'logic/users/User';

const TestTimestamp = ({ timestamp }: { timestamp: string }) => {
  const { unifyTime } = useContext(DateTimeContext);

  return (<div>{unifyTime(timestamp)}</div>);
};

const SUT = ({ timestamp, currentUser: user }: { timestamp: string, currentUser: User }) => (
  <CurrentUserContext.Provider value={user}>
    <DateTimeProvider>
      <TestTimestamp timestamp={timestamp} />
    </DateTimeProvider>,
  </CurrentUserContext.Provider>
);

describe('DateTimeProvider', () => {
  it('should provide a function which converts a date time string to local time of user', () => {
    const user = alice.toBuilder().timezone('Asia/Tokyo').build();

    render(<SUT timestamp="2021-03-27T14:32:31.894Z" currentUser={user} />);

    expect(screen.getByText('2021-03-27 23:32:31.894 +09:00')).toBeInTheDocument();
  });
});
