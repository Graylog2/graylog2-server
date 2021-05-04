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
import PropTypes from 'prop-types';
import React from 'react';

import DateTime from 'logic/datetimes/DateTime';

export const formatDateTime = (dateTime, format, tz, relative = false) => {
  if (relative) {
    return dateTime.toRelativeString();
  }

  switch (tz) {
    case null:
    case undefined:
      return dateTime.toString(format);
    case 'browser':
      return dateTime.toBrowserLocalTime().toString(format);
    default:
      return dateTime.toTimeZone(tz).toString(format);
  }
};

/**
 * Component that renders a `time` HTML element with a given date time. It is
 * capable of render date times in different formats, accepting ISO 8601
 * strings, JS native Date objects, and Moment.js Date objects.
 *
 * The component can display the date time in different formats, and also can
 * show the relative time from/until now.
 *
 * It is also possible to change the time zone for the given date, something
 * that helps, for instance, to display a local time from a UTC time that
 * was used in the server.
 *
 */
class Timestamp extends React.Component {
  static propTypes = {
    /**
     * Date time to be displayed in the component. You can provide an ISO
     * 8601 string, a JS native `Date` object, a moment `Date` object, or
     * a number containing seconds after UNIX epoch.
     */
    dateTime: PropTypes.oneOfType([PropTypes.string, PropTypes.object, PropTypes.number]).isRequired,
    /**
     * Format to use to represent the date time. It supports any format
     * supported by momentjs http://momentjs.com/docs/#/displaying/format/.
     * We also provide a list of default formats in 'logic/datetimes/DateTime':
     *
     *  - DATE: `YYYY-MM-DD`
     *  - DATETIME: `YYYY-MM-DD HH:mm:ss`, local times when decimal second precision is not important
     *  - DATETIME_TZ: `YYYY-MM-DD HH:mm:ss Z`, when decimal second precision is not important, but TZ is
     *  - TIMESTAMP: `YYYY-MM-DD HH:mm:ss.SSS`, local times when decimal second precision is important (e.g. search results)
     *  - TIMESTAMP_TZ: `YYYY-MM-DD HH:mm:ss.SSS Z`, when decimal second precision is important, in a different TZ
     *  - COMPLETE: `dddd D MMMM YYYY, HH:mm ZZ`, easy to read date time
     *  - ISO_8601: `YYYY-MM-DDTHH:mm:ss.SSSZ`
     */
    format: PropTypes.string,
    /** Specifies if the component should display relative time or not. */
    relative: PropTypes.bool,
    /**
     * Specifies the timezone to convert `dateTime`. Use `browser` to
     * convert the date time to the browser's local time, or one of the
     * time zones supported by moment timezone.
     */
    tz: PropTypes.string,
    /**
     * Override render default function to add a decorator.
     */
    render: PropTypes.func,
  };

  static defaultProps = {
    format: DateTime.Formats.TIMESTAMP,
    relative: false,
    tz: undefined,
    render: ({ value }) => value,
  };

  _formatDateTime = () => {
    const { relative, dateTime: dateTime1, tz, format } = this.props;
    const dateTime = new DateTime(dateTime1);

    return formatDateTime(dateTime, format, tz, relative);
  };

  render() {
    const { render: Component, dateTime } = this.props;

    return (
      <time key={`time-${dateTime}`} dateTime={dateTime} title={dateTime}>
        <Component value={this._formatDateTime()} />
      </time>
    );
  }
}

export default Timestamp;
