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
import { useCallback, useMemo } from 'react';

import UserDateTimeContext from 'contexts/UserDateTimeContext';
import AppConfig from 'util/AppConfig';
import useCurrentUser from 'hooks/useCurrentUser';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { DATE_TIME_FORMATS, getBrowserTimezone, toDateObject } from 'util/DateTime';

type Props = {
  children: React.ReactNode,
  tz?: string,
};

const getUserTimezone = (userTimezone: string, tzOverride?: string) => {
  if (tzOverride) {
    return tzOverride;
  }

  return userTimezone ?? getBrowserTimezone() ?? AppConfig.rootTimeZone() ?? 'UTC';
};

/**
 * Provides methods to convert times based on the user time zone.
 * Should be used when displaying times and the related components are not a suitable option.
 *
 * userTimezone   - time zone of the current user.
 * formatTime     - method which takes a date and optionally a format and returns it as a string in the current user timezone.
 *                  For example, it transforms `2010-07-30T16:03:25.000Z` to `2010-07-30 17:03:25` for a user with the timezone `Europe/Berlin`.
 * toUserTimezone - method which takes a date and transforms it a moment date object, based on the user timezone.
 */

const StaticTimezoneProvider = ({ children, tz }: Required<Props>) => {
  const toUserTimezone = useCallback((time: DateTime) => toDateObject(time, undefined, tz), [tz]);

  const formatTime = useCallback((time: DateTime, format: DateTimeFormats = 'default') => toUserTimezone(time).format(DATE_TIME_FORMATS[format]), [toUserTimezone]);

  const contextValue = useMemo(
    () => ({ toUserTimezone, formatTime, userTimezone: tz }),
    [toUserTimezone, formatTime, tz],
  );

  return (
    <UserDateTimeContext.Provider value={contextValue}>
      {children}
    </UserDateTimeContext.Provider>
  );
};

const CurrentUserDateTimeProvider = ({ children }: Omit<Props, 'tz'>) => {
  const currentUser = useCurrentUser();
  const userTimezone = useMemo(() => getUserTimezone(currentUser?.timezone), [currentUser?.timezone]);

  return (
    <StaticTimezoneProvider tz={userTimezone}>
      {children}
    </StaticTimezoneProvider>
  );
};

const UserDateTimeProvider = ({ children, tz: tzOverride }: Props) => (tzOverride
  ? (
    <StaticTimezoneProvider tz={tzOverride}>
      {children}
    </StaticTimezoneProvider>
  )
  : (
    <CurrentUserDateTimeProvider>
      {children}
    </CurrentUserDateTimeProvider>
  )
);

export default UserDateTimeProvider;
