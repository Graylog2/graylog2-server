/* eslint-disable no-console */
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
import { render } from 'wrappedTestingLibrary';
import moment from 'moment';

import { DATE_TIME_FORMATS } from 'util/DateTime';
import { adminUser } from 'fixtures/users';
import UserDateTimeProvider from 'contexts/UserDateTimeProvider';
import UserDateTimeContext from 'contexts/UserDateTimeContext';
import useCurrentUser from 'hooks/useCurrentUser';
import { asMock } from 'helpers/mocking';

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
  .setSystemTime(mockedUnixTime);

jest.mock('hooks/useCurrentUser');

describe('DateTimeProvider', () => {
  const invalidDate = '2020-00-00T04:00:00.000Z';
  const expectErrorForInvalidDate = () => expect(console.error).toHaveBeenCalledWith(`Date time ${invalidDate} is not valid.`);

  const renderSUT = (tzOverride = undefined) => {
    let contextValue;

    render(
      <UserDateTimeProvider tz={tzOverride}>
        <UserDateTimeContext.Consumer>
          {(value) => {
            contextValue = value;

            return <div />;
          }}
        </UserDateTimeContext.Consumer>
      </UserDateTimeProvider>,
    );

    return contextValue;
  };

  const original = console.error;

  beforeEach(() => {
    console.error = jest.fn();
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().timezone('Europe/Berlin').build());
  });

  afterEach(() => {
    console.error = original;
  });

  describe('userTimezone value should', () => {
    it('provide user timezone', () => {
      const { userTimezone } = renderSUT();

      expect(userTimezone).toBe('Europe/Berlin');
    });

    it('respect timezone override', () => {
      const { userTimezone } = renderSUT('America/Los_Angeles');

      expect(userTimezone).toBe('America/Los_Angeles');
    });
  });

  describe('formatTime method should', () => {
    it('convert date to user time zone', () => {
      const { formatTime } = renderSUT();

      expect(formatTime('2021-03-27T14:32:31.894Z')).toBe('2021-03-27 15:32:31');
    });

    it('log an error for an invalid date', () => {
      const { formatTime } = renderSUT();
      formatTime(invalidDate);
      expectErrorForInvalidDate();
    });

    it('respect timezone override', () => {
      const { formatTime } = renderSUT('America/Los_Angeles');

      expect(formatTime('2021-03-27T14:32:31.894Z')).toBe('2021-03-27 07:32:31');
    });
  });

  describe('toUserTimeZone method should', () => {
    it('convert date to moment date with user time zone', () => {
      const { toUserTimezone } = renderSUT();
      const result = toUserTimezone('2021-03-27T14:32:31.894Z');

      expect(moment.isMoment(result)).toBe(true);
      expect(result.format(DATE_TIME_FORMATS.internal)).toBe('2021-03-27T15:32:31.894+01:00');
    });

    it('log an error for an invalid date', () => {
      const { toUserTimezone } = renderSUT();
      toUserTimezone(invalidDate);
      expectErrorForInvalidDate();
    });

    it('respect timezone override', () => {
      const { toUserTimezone } = renderSUT('America/Los_Angeles');
      const result = toUserTimezone('2021-03-27T14:32:31.894Z');

      expect(moment.isMoment(result)).toBe(true);
      expect(result.format(DATE_TIME_FORMATS.internal)).toBe('2021-03-27T07:32:31.894-07:00');
    });
  });
});
