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

import { alice } from 'fixtures/users';
import DateTimeProvider, { DATE_TIME_FORMATS } from 'contexts/DateTimeProvider';
import DateTimeContext from 'contexts/DateTimeContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import User from 'logic/users/User';

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
  .setSystemTime(mockedUnixTime);

describe('DateTimeProvider', () => {
  const user = alice.toBuilder().timezone('Europe/Berlin').build();

  const renderSUT = (currentUser: User = user) => {
    let contextValue;

    render(
      <CurrentUserContext.Provider value={currentUser}>
        <DateTimeProvider>
          <DateTimeContext.Consumer>
            {(value) => {
              contextValue = value;

              return <div />;
            }}
          </DateTimeContext.Consumer>
        </DateTimeProvider>,
      </CurrentUserContext.Provider>,
    );

    return contextValue;
  };

  it('should provider user timezone', () => {
    const { userTimezone } = renderSUT();

    expect(userTimezone).toBe('Europe/Berlin');
  });

  describe('formatTime method should', () => {
    describe('convert time to time zone', () => {
      it('user time zone by default', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z')).toBe('2021-03-27 15:32:31');
      });

      it('specified time zone', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z', 'US/Alaska')).toBe('2021-03-27 06:32:31');
      });
    });

    describe('converts different types of date times', () => {
      it('date time string', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z')).toBe('2021-03-27 15:32:31');
      });

      it('JS date', () => {
        const { formatTime } = renderSUT();

        expect(formatTime(new Date('2021-03-27T14:32:31.894Z'))).toBe('2021-03-27 15:32:31');
      });

      it('unix timestamp', () => {
        const { formatTime } = renderSUT();

        expect(formatTime(1616855551894)).toBe('2021-03-27 15:32:31');
      });

      it('moment object', () => {
        const { formatTime } = renderSUT();

        expect(formatTime(moment('2021-03-27T14:32:31.894Z'))).toBe('2021-03-27 15:32:31');
      });
    });

    describe('convert date time to a specific format', () => {
      it('default', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z', undefined, 'default')).toBe('2021-03-27 15:32:31');
      });

      it('complete', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z', undefined, 'complete')).toBe('2021-03-27 15:32:31.894');
      });

      it('with time zone', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z', undefined, 'withTZ')).toBe('2021-03-27T15:32:31+01:00');
      });

      it('readable', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z', undefined, 'readable')).toBe('Saturday 27 March 2021, 15:32 +0100');
      });

      it('internal', () => {
        const { formatTime } = renderSUT();

        expect(formatTime('2021-03-27T14:32:31.894Z', undefined, 'internal')).toBe('2021-03-27T14:32:31.894Z');
      });
    });

    describe('adjustTimezone method should', () => {
      const format = (dateTime) => dateTime.format(DATE_TIME_FORMATS.complete);

      describe('convert time to time zone', () => {
        it('user time zone by default', () => {
          const { adjustTimezone } = renderSUT();

          expect(format(adjustTimezone('2021-03-27T14:32:31.894Z'))).toBe('2021-03-27 15:32:31.894');
        });

        it('specified time zone', () => {
          const { adjustTimezone } = renderSUT();

          expect(format(adjustTimezone('2021-03-27T14:32:31.894Z', 'US/Alaska'))).toBe('2021-03-27 06:32:31.894');
        });
      });

      describe('converts different types of date times', () => {
        it('date time string', () => {
          const { adjustTimezone } = renderSUT();

          expect(format(adjustTimezone('2021-03-27T14:32:31.894Z'))).toBe('2021-03-27 15:32:31.894');
        });

        it('JS date', () => {
          const { adjustTimezone } = renderSUT();

          expect(format(adjustTimezone(new Date('2021-03-27T14:32:31.894Z')))).toBe('2021-03-27 15:32:31.894');
        });

        it('unix timestamp', () => {
          const { adjustTimezone } = renderSUT();

          expect(format(adjustTimezone(1616855551894))).toBe('2021-03-27 15:32:31.894');
        });

        it('moment object', () => {
          const { adjustTimezone } = renderSUT();

          expect(format(adjustTimezone(moment('2021-03-27T14:32:31.894Z')))).toBe('2021-03-27 15:32:31.894');
        });
      });
    });

    describe('formatAsBrowserTime method should', () => {
      it('format date time to browser timezone', () => {
        const { formatAsBrowserTime } = renderSUT();

        expect(formatAsBrowserTime('2021-03-27T14:32:31.894Z')).toBe('2021-03-27 09:32:31"');
      });

      it('format date time to browser timezone with specific format', () => {
        const { formatAsBrowserTime } = renderSUT();

        expect(formatAsBrowserTime('2021-03-27T14:32:31.894Z', 'complete')).toBe('2021-03-27 09:32:31.894');
      });
    });

    it('relativeDifference method should return relative time difference for date', () => {
      const { relativeDifference } = renderSUT();

      expect(relativeDifference('2020-01-01T10:00:00.000Z')).toBe('in 10 hours');
    });
  });
});
