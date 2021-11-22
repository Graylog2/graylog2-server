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
  default: 'YYYY-MM-DD HH:mm:ss', // default format when displaying date times
  complete: 'YYYY-MM-DD HH:mm:ss.SSS', // includes ms, useful were precise time is important
  withTz: 'YYYY-MM-DD HH:mm:ss Z', // includes the time zone
  readable: 'dddd D MMMM YYYY, HH:mm ZZ', // easy to read
  internal: 'YYYY-MM-DDTHH:mm:ss.SSSZ', // ISO 8601, internal default, not really nice to read. Mostly used communication with the API.
  date: 'YYYY-MM-DD',
};

/**
 * Provides a function to convert a ACCEPTED_FORMAT of a timestamp into a timestamp converted to the users timezone.
 * @param children React components.
 */
const DateTimeProvider = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userTimezone = useMemo(() => getUserTimezone(currentUser?.timezone), [currentUser?.timezone]);

  const adjustTimezone = useCallback((time, tz = userTimezone) => {
    return moment.tz(time, tz);
  }, [userTimezone]);

  const formatTime = useCallback((time, tz = userTimezone, format = 'default') => {
    return adjustTimezone(time, tz).format(DATE_TIME_FORMATS[format]);
  }, [adjustTimezone, userTimezone]);

  const formatAsBrowserTime = (time, format) => {
    return formatTime(time, getBrowserTimezone(), format);
  };

  const relativeDifference = (time) => {
    return moment(time).fromNow();
  };

  const contextValue = {
    formatTime,
    adjustTimezone,
    userTimezone,
    formatAsBrowserTime,
    relativeDifference,
  };

  return (
    <DateTimeContext.Provider value={contextValue}>
      {children}
    </DateTimeContext.Provider>
  );
};

export default DateTimeProvider;
