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
import { useCallback, useContext } from 'react';
import moment from 'moment-timezone';

import TimeLocalizeContext, { TimeLocalizeContextType } from 'contexts/TimeLocalizeContext';
import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';

type Props = {
  children: React.ReactChildren | React.ReactChild | ((timeLocalize: TimeLocalizeContextType) => Element),
};

export const ACCEPTED_FORMATS = {
  DATE: 'YYYY-MM-DD',
  TIME: 'HH:mm:ss.SSS',
  DATETIME: 'YYYY-MM-DD HH:mm:ss', // Use to show local times when decimal second precision is not important
  DATETIME_TZ: 'YYYY-MM-DD HH:mm:ss Z', // Use when decimal second precision is not important, but TZ is
  TIMESTAMP: 'YYYY-MM-DD HH:mm:ss.SSS', // Use to show local time & date when decimal second precision is important
  // (e.g. search results)
  TIMESTAMP_TZ: 'YYYY-MM-DD HH:mm:ss.SSS Z', // Use to show times when decimal second precision is important, in a different TZ
  COMPLETE: 'dddd D MMMM YYYY, HH:mm ZZ', // Easy to read date time, specially useful for graphs
  ISO_8601: 'YYYY-MM-DDTHH:mm:ss.SSSZ', // Standard, but not really nice to read. Mostly used for machine communication
};

/**
 * Provides a function to convert a ACCEPTED_FORMAT of a timestamp into a timestamp converted to the users timezone.
 * @param children React components.
 */
const TimeLocalizeProvider = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);

  const getUserTimezone = useCallback(() => {
    if (currentUser?.timezone) {
      return currentUser.timezone;
    }

    return moment.tz.guess() || AppConfig.rootTimeZone() || 'UTC';
  }, [currentUser.timezone]);

  const localizeTime = useCallback((time) => {
    const dateTime = moment.tz(time.trim(), Object.values(ACCEPTED_FORMATS), true, getUserTimezone());

    return dateTime.format(ACCEPTED_FORMATS.TIMESTAMP_TZ);
  }, [getUserTimezone]);

  return (
    <TimeLocalizeContext.Provider value={{ localizeTime }}>
      {children}
    </TimeLocalizeContext.Provider>
  );
};

export default TimeLocalizeProvider;
