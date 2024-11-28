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

// This file provides utility functions to handle times. By default, most functions return a UTC date.

export const DATE_TIME_FORMATS = {
  default: 'YYYY-MM-DD HH:mm:ss', // default format when displaying date times
  complete: 'YYYY-MM-DD HH:mm:ss.SSS', // includes ms, useful were precise time is important
  withTz: 'YYYY-MM-DD HH:mm:ss Z', // includes the time zone
  readable: 'dddd D MMMM YYYY, HH:mm ZZ', // easy to read
  shortReadable: 'MMM D, YYYY', // easy to read
  internal: 'YYYY-MM-DDTHH:mm:ss.SSSZ', // ISO 8601, internal default, not really nice to read. Mostly used communication with the API.
  internalIndexer: 'YYYY-MM-DDTHH:mm:ss.SSS[Z]', // ISO 8601, used for ES search queries, when a timestamp has to be reformatted
  date: 'YYYY-MM-DD',
  hourAndMinute: 'HH:mm',
  dateHourAndMinute: 'YYYY-MM-DD HH:mm',
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

const getFormatStringsForDateTimeFormats = (dateTimeFormats: Array<DateTimeFormats>) => dateTimeFormats?.map((dateTimeFormat) => {
  const format = DATE_TIME_FORMATS[dateTimeFormat];

  if (!format) {
    throw new Error(`Provided date time format "${dateTimeFormat}" is not supported.`);
  }

  return format;
});

/**
 * Takes a date and returns it as a moment object. Optionally you can define a time zone, which will be considered when displaying the date.
 * You can also define `acceptedFormats` in case you want to throw an error if the provided date does not match an expected format.
 */
export const toDateObject = (dateTime: DateTime, acceptedFormats?: Array<DateTimeFormats>, tz = DEFAULT_OUTPUT_TZ) => {
  const acceptedFormatStrings = getFormatStringsForDateTimeFormats(acceptedFormats);
  const dateObject = moment.utc(dateTime, acceptedFormatStrings, true).tz(tz);
  const validationInfo = acceptedFormats?.length ? `Expected formats: ${acceptedFormatStrings.join(', ')}.` : undefined;

  return validateDateTime(dateObject, dateTime, validationInfo);
};

/**
 * Transforms an ISO 8601 date time to a moment date object. It throws an error if the provided date time is not expressed according to ISO 8601.
 */
export const parseFromIsoString = (dateTimeString: string, tz = DEFAULT_OUTPUT_TZ) => toDateObject(dateTimeString, ['internal'], tz);

/**
 * Returns the estimated browser time zone.
 */
export const getBrowserTimezone = () => moment.tz.guess();

/**
 * Returns the provided date time as a string, based on the targeted format and timezone.
 */
export const adjustFormat = (dateTime: DateTime, format: DateTimeFormats = 'default', tz = DEFAULT_OUTPUT_TZ) => toDateObject(dateTime, undefined, tz).format(DATE_TIME_FORMATS[format]);

/**
 * Returns the provided date time as a string, based on the targeted format and the browser timezone.
 */
export const formatAsBrowserTime = (time: DateTime, format: DateTimeFormats = 'default') => adjustFormat(time, format, getBrowserTimezone());

/**
 * Returns the time in a human-readable format, relative to the provided date time.
 * If you just want to display the output, you can use the `RelativeTime` component.
 */
export const relativeDifference = (dateTime: DateTime) => {
  const dateObject = toDateObject(dateTime);

  return validateDateTime(dateObject, dateTime).fromNow();
};

/**
 * Returns the time difference, relative to the provided date time, in days.
 */
export const relativeDifferenceDays = (dateTime: DateTime) => {
  const eventDateObject = toDateObject(dateTime);
  const todayDateObject = toDateObject(new Date());

  return todayDateObject.diff(eventDateObject, 'days');
};

/**
 * Returns the time difference, relative to the provided date time, in seconds.
 */
export const relativeDifferenceSeconds = (dateTime: DateTime) => {
  const eventDateObject = toDateObject(dateTime);
  const todayDateObject = toDateObject(new Date());

  return todayDateObject.diff(eventDateObject, 'seconds');
};

/**
 * Validate if the provided time has a supported format.
 */
export const isValidDate = (dateTime: DateTime) => moment(dateTime, Object.values(DATE_TIME_FORMATS), true).isValid();

/**
 * This function transforms the provided date time to UTC, based on the defined time zone.
 * This is useful for date times like `2010-01-01 10:00:00`, which do not include the timezone information.
 * For this calculation it is necessary to define the timezone which the date time is currently based on.
 */
export const toUTCFromTz = (dateTime: string, sourceTimezone: string) => {
  if (!sourceTimezone) {
    throw new Error('It is required to define the time zone of the date time provided for internalUTCTime.');
  }

  return moment.tz(dateTime, sourceTimezone);
};

/**
 * Takes a duration (e.g. in milliseconds or seconds, or as a ISO8601 duration) and returns it in seconds.
 */
export const durationInSeconds = (duration: string | number) => moment.duration(duration).asSeconds();

/**
 * Takes a duration (e.g. in milliseconds or seconds, or as a ISO8601 duration) and returns it in minutes.
 */
export const durationInMinutes = (duration: string | number) => moment.duration(duration).asMinutes();
/**
 * Takes a duration (e.g. in minutes or seconds, or as a ISO8601 duration) and returns it in milliseconds.
 */
export const durationToMS = (duration: string) => moment.duration(duration).asMilliseconds();
