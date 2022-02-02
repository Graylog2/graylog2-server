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
import { useCallback, useMemo, useContext } from 'react';

import UserDateTimeContext from 'contexts/UserDateTimeContext';
import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { DATE_TIME_FORMATS, getBrowserTimezone, toDateObject } from 'util/DateTime';

type Props = {
  children: React.ReactNode,
  tzOverride?: string,
};

const getUserTimezone = (userTimezone, tzOverride) => {
  if (tzOverride) {
    return tzOverride;
  }

  return userTimezone ?? getBrowserTimezone() ?? AppConfig.rootTimeZone() ?? 'UTC';
};

/**
 * Provides methods to convert times based on the user time zone.
 * Should be used when displaying times and the related components are not a suitable option.
 */

const UserDateTimeProvider = ({ children, tzOverride }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userTimezone = useMemo(() => getUserTimezone(currentUser?.timezone, tzOverride), [currentUser?.timezone, tzOverride]);

  const toUserTimezone = useCallback((time: DateTime) => {
    return toDateObject(time, undefined, userTimezone);
  }, [userTimezone]);

  const formatTime = useCallback((time: DateTime, format: DateTimeFormats = 'default') => {
    return toUserTimezone(time).format(DATE_TIME_FORMATS[format]);
  }, [toUserTimezone]);

  const contextValue = useMemo(
    () => ({ toUserTimezone, formatTime, userTimezone }),
    [toUserTimezone, formatTime, userTimezone],
  );

  return (
    <UserDateTimeContext.Provider value={contextValue}>
      {children}
    </UserDateTimeContext.Provider>
  );
};

UserDateTimeProvider.defaultProps = {
  tzOverride: undefined,
};

export default UserDateTimeProvider;
