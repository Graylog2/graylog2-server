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

// This file provides utility functions to handle times. Most functions expect a UTC date, if the `

export const DATE_TIME_FORMATS = {
  default: 'YYYY-MM-DD HH:mm:ss', // default format when displaying date times
  complete: 'YYYY-MM-DD HH:mm:ss.SSS', // includes ms, useful were precise time is important
  withTz: 'YYYY-MM-DD HH:mm:ss Z', // includes the time zone
  readable: 'dddd D MMMM YYYY, HH:mm ZZ', // easy to read
  internal: 'YYYY-MM-DDTHH:mm:ss.SSSZ', // ISO 8601, internal default, not really nice to read. Mostly used communication with the API.
  date: 'YYYY-MM-DD',
};

const DEFAULT_OUTPUT_TZ = 'UTC';

const validateDateTime = (dateTime: Moment, originalDateTime: DateTime, additionalInfo?: string) => {
  if (!dateTime.isValid()) {
    let errorMessage = `Date time ${originalDateTime} is not valid.`;

    if (additionalInfo) {
      errorMessage = `${errorMessage} ${additionalInfo}`;
    }

    throw new Error(errorMessage);
  }

  return dateTime;
};

export const toDateObject = (dateTime: DateTime, acceptedFormats?: Array<string>, tz = DEFAULT_OUTPUT_TZ) => {
  const dateObject = moment(dateTime, acceptedFormats, true).tz(tz);
  const validationInfo = acceptedFormats?.length ? `Expected formats: ${acceptedFormats.join(', ')}.` : undefined;

  return validateDateTime(dateObject, dateTime, validationInfo);
};

export const parseFromIsoString = (dateTimeString: string, tz = DEFAULT_OUTPUT_TZ) => {
  return toDateObject(dateTimeString, [DATE_TIME_FORMATS.internal], tz);
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
