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
import moment from 'moment-timezone';

export type DateTime = string | number | Moment | Date;

export type DateTimeFormats = keyof typeof DATE_TIME_FORMATS;

// This file provides utility functions to handle times. By default most functions return a UTC date.

export const DATE_TIME_FORMATS = {
  default: 'YYYY-MM-DD HH:mm:ss', // default format when displaying date times
  complete: 'YYYY-MM-DD HH:mm:ss.SSS', // includes ms, useful were precise time is important
  withTz: 'YYYY-MM-DD HH:mm:ss Z', // includes the time zone
  readable: 'dddd D MMMM YYYY, HH:mm ZZ', // easy to read
  internal: 'YYYY-MM-DDTHH:mm:ss.SSSZ', // ISO 8601, internal default, not really nice to read. Mostly used communication with the API.
  internalIndexer: 'YYYY-MM-DDTHH:mm:ss.SSS[Z]', // ISO 8601, used for ES search queries, when a timestamp has to be reformatted
  date: 'YYYY-MM-DD',
};

const DEFAULT_OUTPUT_TZ = 'UTC';

const validateDateTime = (dateTime: Moment, originalDateTime: DateTime, additionalInfo?: string) => {
  if (!dateTime.isValid()) {
    let errorMessage = `Date time ${originalDateTime} is not valid.`;

    if (additionalInfo) {
      errorMessage = `${errorMessage} ${additionalInfo}`;
    }

    // eslint-disable-next-line no-console
    console.error(errorMessage);
  }

  return dateTime;
};

const getFormatStringsForDateTimeFormats = (dateTimeFormats: Array<DateTimeFormats>) => {
  return dateTimeFormats?.map((dateTimeFormat) => {
    const format = DATE_TIME_FORMATS[dateTimeFormat];

    if (!format) {
      throw new Error(`Provided date time format "${dateTimeFormat}" is not supported.`);
    }

    return format;
  });
};

export const toDateObject = (dateTime: DateTime, acceptedFormats?: Array<DateTimeFormats>, tz = DEFAULT_OUTPUT_TZ) => {
  const acceptedFormatStrings = getFormatStringsForDateTimeFormats(acceptedFormats);
  const dateObject = moment(dateTime, acceptedFormatStrings, true).tz(tz);
  const validationInfo = acceptedFormats?.length ? `Expected formats: ${acceptedFormatStrings.join(', ')}.` : undefined;

  return validateDateTime(dateObject, dateTime, validationInfo);
};

export const parseFromIsoString = (dateTimeString: string, tz = DEFAULT_OUTPUT_TZ) => {
  return toDateObject(dateTimeString, ['internal'], tz);
};

export const getBrowserTimezone = () => {
  return moment.tz.guess();
};

export const adjustFormat = (dateTime: DateTime, format: DateTimeFormats = 'default', tz = DEFAULT_OUTPUT_TZ) => {
  return toDateObject(dateTime, undefined, tz).format(DATE_TIME_FORMATS[format]);
};

export const formatAsBrowserTime = (time: DateTime, format: DateTimeFormats = 'default') => {
  return adjustFormat(time, format, getBrowserTimezone());
};

export const relativeDifference = (dateTime: DateTime) => {
  const dateObject = toDateObject(dateTime);

  return validateDateTime(dateObject, dateTime).fromNow();
};

export const isValidDate = (dateTime: DateTime) => moment(dateTime, Object.values(DATE_TIME_FORMATS), true).isValid();

// This function allows transforming a date time, which does not contain a time zone like `2010-01-01 10:00:00`, to UTC.
// For this calculation it is necessary to define the time zone of the provided date time.
export const toUTCFromTz = (dateTime: string, sourceTimezone: string) => {
  if (!sourceTimezone) {
    throw new Error('It is required to define the time zone of the date time provided for internalUTCTime.');
  }

  return moment.tz(dateTime, sourceTimezone);
};
