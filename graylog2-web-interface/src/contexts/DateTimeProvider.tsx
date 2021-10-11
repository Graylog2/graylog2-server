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
import moment from 'moment-timezone';

import DateTimeContext, { DateTimeContextType } from 'contexts/DateTimeContext';
import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';

type Props = {
  children: React.ReactChildren | React.ReactChild | ((timeLocalize: DateTimeContextType) => Element),
};

const getBrowserTimezone = () => {
  return moment.tz.guess();
};

const getUserTimezone = (userTimezone) => {
  return userTimezone ?? getBrowserTimezone() ?? AppConfig.rootTimeZone() ?? 'UTC';
};

export const DATE_TIME_FORMATS = {
  default: 'YYYY-MM-DD HH:mm:ss',
  complete: 'YYYY-MM-DD HH:mm:ss.SSS',
  withTz: 'YYYY-MM-DD HH:mm:ss Z',
  readable: 'dddd D MMMM YYYY, HH:mm ZZ',
};

/**
 * Provides a function to convert a ACCEPTED_FORMAT of a timestamp into a timestamp converted to the users timezone.
 * @param children React components.
 */
const DateTimeProvider = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userTimezone = useMemo(() => getUserTimezone(currentUser?.timezone), [currentUser?.timezone]);

  const unifyTimeAsDate = useCallback((time, tz = userTimezone) => {
    return moment.tz(time, tz);
  }, [userTimezone]);

  const unifyTime = useCallback((time, tz = userTimezone, format = 'default') => {
    return unifyTimeAsDate(time, tz).format(DATE_TIME_FORMATS[format]);
  }, [unifyTimeAsDate, userTimezone]);

  const unifyAsBrowserTime = (time, format) => {
    return unifyTime(time, getBrowserTimezone(), format);
  };

  const relativeDifference = (time, tz) => {
    return unifyTimeAsDate(time, tz).fromNow();
  };

  const contextValue = {
    unifyTime,
    unifyTimeAsDate,
    userTimezone,
    unifyAsBrowserTime,
    relativeDifference,
  };

  return (
    <DateTimeContext.Provider value={contextValue}>
      {children}
    </DateTimeContext.Provider>
  );
};

export default DateTimeProvider;
