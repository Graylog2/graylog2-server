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
import moment from 'moment-timezone';

import AppConfig from 'util/AppConfig';
import StoreProvider from 'injection/StoreProvider';

class DateTime {
  private dateTime: moment.Moment | undefined;

  static get Formats() {
    return {
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
  }

  // Discards TZ information and treats date time as user's local. Helpful when getting a Javascript Date object
  static ignoreTZ(dateTime) {
    return new DateTime(moment(dateTime).format(DateTime.Formats.TIMESTAMP));
  }

  // Converts UTC without TZ information into user's local time
  static fromUTCDateTime(dateTime) {
    return new DateTime(moment.utc(dateTime));
  }

  static _cleanDateTimeString(dateTimeString) {
    if (dateTimeString instanceof String) {
      return dateTimeString.trim();
    }

    return dateTimeString;
  }

  static getAcceptedFormats() {
    // First date format matching the date wins, so we order them by strictness
    return [
      DateTime.Formats.ISO_8601,
      DateTime.Formats.TIMESTAMP_TZ,
      DateTime.Formats.DATETIME_TZ,
      DateTime.Formats.TIMESTAMP,
      DateTime.Formats.DATETIME,
      DateTime.Formats.COMPLETE,
      DateTime.Formats.DATE,
    ];
  }

  // Tries to parse the given string using `.getAcceptedFormats`
  static parseFromString(dateTimeString) {
    const parsedDateTime = moment.tz(DateTime._cleanDateTimeString(dateTimeString), DateTime.getAcceptedFormats(), true, DateTime.getUserTimezone());

    if (!parsedDateTime.isValid()) {
      throw new Error(`Date time ${dateTimeString} is not valid`);
    }

    return new DateTime(parsedDateTime);
  }

  static isValidDateString(dateTimeString) {
    const parsedDateTime = moment.tz(DateTime._cleanDateTimeString(dateTimeString), DateTime.getAcceptedFormats(), true, DateTime.getUserTimezone());

    return parsedDateTime.isValid();
  }

  private static currentUserStoreUnsub: (() => void) | undefined;

  private static _currentUser: any;

  static getCurrentUser() {
    if (!this.currentUserStoreUnsub) {
      const CurrentUserStore = StoreProvider.getStore('CurrentUser');

      this._currentUser = CurrentUserStore.get();

      this.currentUserStoreUnsub = CurrentUserStore.listen((state) => { this._currentUser = state.currentUser; });
    }

    return this._currentUser;
  }

  static getUserTimezone() {
    const currentUser = this.getCurrentUser();

    if (currentUser?.timezone) {
      return currentUser.timezone;
    }

    return this.getBrowserTimezone() || AppConfig.rootTimeZone() || 'UTC';
  }

  static getBrowserTimezone() {
    return moment.tz.guess();
  }

  constructor(dateTime) {
    if (!dateTime) {
      this.dateTime = DateTime.now();

      return;
    }

    // Always use user's local time
    this.dateTime = moment.tz(dateTime, DateTime.getUserTimezone());
  }

  static now() {
    return moment.tz(moment(), DateTime.getUserTimezone());
  }

  // Converts the DateTime to the user's browser local time
  toBrowserLocalTime() {
    const localOffset = (new Date()).getTimezoneOffset();

    this.dateTime.utcOffset(-localOffset);

    return this;
  }

  // Changes the timezone of the DateTime
  toTimeZone(tz) {
    this.dateTime.tz(tz);

    return this;
  }

  toRelativeString() {
    return this.dateTime.fromNow();
  }

  // Returns internal moment object
  toMoment() {
    return this.dateTime;
  }

  // Returns a Javascript Date object
  toDate() {
    return this.dateTime.toDate();
  }

  // Returns an ISO_8601 formatted string
  toISOString() {
    return this.dateTime.toISOString();
  }

  toString(format) {
    let effectiveFormat = format;

    if (format === undefined || format === null) {
      if (this.dateTime.milliseconds() === 0) {
        effectiveFormat = DateTime.Formats.DATETIME;
      } else {
        effectiveFormat = DateTime.Formats.TIMESTAMP;
      }
    }

    return this.dateTime.format(effectiveFormat);
  }
}

export default DateTime;
