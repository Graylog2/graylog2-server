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
import type { Moment } from 'moment';

import type { DateTimeFormats } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';
import { adjustFormat } from 'util/DateTime';

type Props = {
  dateTime?: string | number | Date | Moment,
  field?: string,
  format?: DateTimeFormats,
  render?: React.ComponentType<{ value: string, field: string | undefined }>,
  tz?: string,
}

/**
 * Component that renders a given date time based on the user time zone in a `time` HTML element.
 * It is capable of render date times in different formats, accepting ISO 8601
 * strings, JS native Date objects, and Moment.js Date objects.
 *
 * While the component is using the user time zone by default, it is also possible
 * to change the time zone for the given date, something that helps, for instance, to display a local time
 * from a UTC time that was used in the server.
 *
 */
const Timestamp = ({ dateTime: dateTimeProp, field, format, render: Component, tz }: Props) => {
  const { formatTime: formatWithUserTz } = useUserDateTime();
  const dateTime = dateTimeProp ?? new Date();
  const formattedDateTime = tz ? adjustFormat(dateTime, format, tz) : formatWithUserTz(dateTime, format);
  const dateTimeString = adjustFormat(dateTime, 'internal');

  return (
    <time dateTime={dateTimeString} title={dateTimeString}>
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
  dateTime: PropTypes.oneOfType([PropTypes.string, PropTypes.object, PropTypes.number]),
  /**
   * Format to represent the date time.
   */
  format: PropTypes.string,
  /** Provides field prop for the render function. */
  field: PropTypes.string,
  /**
   * Specifies the time zone to convert `dateTime` to.
   * If not defined the user zone will be used.
   */
  tz: PropTypes.string,
  /**
   * Override render default function to add a decorator.
   */
  render: PropTypes.func,
};

Timestamp.defaultProps = {
  dateTime: undefined,
  field: undefined,
  format: 'default',
  render: ({ value }) => value,
  tz: undefined,
};

export default Timestamp;
