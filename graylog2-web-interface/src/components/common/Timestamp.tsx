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
import { useMemo, useContext } from 'react';
import type { Moment } from 'moment';

import DateTimeContext, { DateTimeFormats } from 'contexts/DateTimeContext';

const formatDateTime = (dateTime, format, tz, relative = false, unifyTime, unifyAsBrowserTime, relativeDifference): string => {
  if (relative) {
    return relativeDifference(dateTime, tz);
  }

  if (tz === 'browser') {
    return unifyAsBrowserTime(dateTime, format);
  }

  return unifyTime(dateTime, tz, format);
};

type Props = {
  dateTime: string | number | Date | Moment,
  field?: string,
  format?: DateTimeFormats,
  relative?: boolean,
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
const Timestamp = ({ dateTime, field, format, relative, render: Component, tz }: Props) => {
  const { unifyTime, unifyAsBrowserTime, relativeDifference } = useContext(DateTimeContext);
  const formattedDateTime = useMemo(
    () => formatDateTime(dateTime, format, tz, relative, unifyTime, unifyAsBrowserTime, relativeDifference),
    [dateTime, format, tz, relative, unifyTime, unifyAsBrowserTime, relativeDifference],
  );

  return (
    <time key={`time-${dateTime}`} dateTime={String(dateTime)} title={String(dateTime)}>
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

Timestamp.defaultProps = {
  field: undefined,
  format: 'default',
  relative: false,
  render: ({ value }) => value,
  tz: undefined,
};

export default Timestamp;
