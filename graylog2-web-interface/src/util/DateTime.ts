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
import type { Moment } from 'moment';
import moment from 'moment';

export type DateTime = string | number | Moment | Date;

export type DateTimeFormats = keyof typeof DATE_TIME_FORMATS;

// This file provides some utility functions to handle with times.
// Please note, whenever you want to display times, use the methods provided by the DateTimeProvider.
// This ensures that we always use unify the way we display times for the user, in terms of the used format and time zone.

export const DATE_TIME_FORMATS = {
  default: 'YYYY-MM-DD HH:mm:ss', // default format when displaying date times
  complete: 'YYYY-MM-DD HH:mm:ss.SSS', // includes ms, useful were precise time is important
  withTz: 'YYYY-MM-DD HH:mm:ss Z', // includes the time zone
  readable: 'dddd D MMMM YYYY, HH:mm ZZ', // easy to read
  internal: 'YYYY-MM-DDTHH:mm:ss.SSSZ', // ISO 8601, internal default, not really nice to read. Mostly used communication with the API.
  date: 'YYYY-MM-DD',
};

export const validateDateTime = (dateTime: Moment, originalDateTime: DateTime, additionalInfo?: string) => {
  if (!dateTime.isValid()) {
    let errorMessage = `Date time ${originalDateTime} is not valid.`;

    if (additionalInfo) {
      errorMessage = `${errorMessage} ${additionalInfo}`;
    }

    throw new Error(errorMessage);
  }

  return dateTime;
};

export const toDateObject = (dateTime: DateTime, acceptedFormats?: Array<string>) => {
  const dateObject = moment(dateTime, acceptedFormats, true);

  return validateDateTime(dateObject, dateTime);
};

export const parseFromIsoString = (dateTimeString: string) => {
  const dateObject = toDateObject(dateTimeString, [DATE_TIME_FORMATS.internal]);

  return validateDateTime(dateObject, dateTimeString, 'Expected ISO 8601 date time.');
};

export const getBrowserTimezone = () => {
  return moment.tz.guess();
};

export const adjustTimezone = (dateTime: DateTime, tz: string = 'UTC') => {
  return validateDateTime(moment.tz(dateTime, tz), dateTime);
};

export const adjustFormat = (dateTime: DateTime, format: DateTimeFormats = 'default') => {
  return toDateObject(dateTime).format(DATE_TIME_FORMATS[format]);
};

export const formatAsBrowserTime = (time: DateTime, format: DateTimeFormats = 'default') => {
  return adjustFormat(adjustTimezone(time, getBrowserTimezone()), format);
};

export const relativeDifference = (dateTime: DateTime) => {
  const dateObject = toDateObject(dateTime);

  return validateDateTime(dateObject, dateTime).fromNow();
};
