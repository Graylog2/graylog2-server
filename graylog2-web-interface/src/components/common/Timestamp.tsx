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
import * as React from 'react';
import { useContext } from 'react';
import type { Moment } from 'moment';

import DateTimeContext from 'contexts/DateTimeContext';
import type { DateTimeFormats } from 'util/DateTime';

type Props = {
  dateTime: string | number | Date | Moment,
  field?: string,
  format?: DateTimeFormats,
  render?: React.ComponentType<{ value: string, field: string | undefined }>,
  tz?: string,
}

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
const Timestamp = ({ dateTime, field, format, render: Component, tz }: Props) => {
  const { formatTime } = useContext(DateTimeContext);
  const formattedDateTime = formatTime(dateTime, tz, format);

  return (
    <time dateTime={String(dateTime)} title={String(dateTime)}>
      <Component value={formattedDateTime} field={field} />
    </time>
  );
};

Timestamp.propTypes = {
  /**
   * Date time to be displayed in the component. You can provide an ISO
   * 8601 string, a JS native `Date` object, a moment `Date` object, or
   * a number containing seconds after UNIX epoch.
   */
  dateTime: PropTypes.oneOfType([PropTypes.string, PropTypes.object, PropTypes.number]).isRequired,
  /**
   * Format to use to represent the date time.
   */
  format: PropTypes.string,
  /** Provides field prop for the render function. */
  field: PropTypes.string,
  /**
   * Specifies the timezone to convert `dateTime`.
   */
  tz: PropTypes.string,
  /**
   * Override render default function to add a decorator.
   */
  render: PropTypes.func,
};

Timestamp.defaultProps = {
  field: undefined,
  format: 'default',
  render: ({ value }) => value,
  tz: undefined,
};

export default Timestamp;
