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

import type { DateTimeContextType } from 'contexts/DateTimeContext';
import DateTimeContext from 'contexts/DateTimeContext';
import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { DATE_TIME_FORMATS, getBrowserTimezone, adjustTimezone } from 'util/DateTime';

type Props = {
  children: React.ReactChildren | React.ReactChild | ((timeLocalize: DateTimeContextType) => Element),
};

const getUserTimezone = (userTimezone) => {
  return userTimezone ?? getBrowserTimezone() ?? AppConfig.rootTimeZone() ?? 'UTC';
};

/**
 * Provides a function to convert a ACCEPTED_FORMAT of a timestamp into a timestamp converted to the users timezone.
 * @param children React components.
 */
const DateTimeProvider = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userTimezone = useMemo(() => getUserTimezone(currentUser?.timezone), [currentUser?.timezone]);

  const toUserTimezone = useCallback((time: DateTime) => {
    return adjustTimezone(time, userTimezone);
  }, [userTimezone]);

  const formatTime = useCallback((time: DateTime, format: DateTimeFormats = 'default') => {
    return toUserTimezone(time).format(DATE_TIME_FORMATS[format]);
  }, [toUserTimezone]);

  const contextValue = {
    toUserTimezone,
    formatTime,
    userTimezone,
  };

  return (
    <DateTimeContext.Provider value={contextValue}>
      {children}
    </DateTimeContext.Provider>
  );
};

export default DateTimeProvider;
