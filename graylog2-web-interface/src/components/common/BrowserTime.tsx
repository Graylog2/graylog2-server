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
import PropTypes from 'prop-types';
import type { Moment } from 'moment';

import type { DateTimeFormats } from 'util/DateTime';
import { formatAsBrowserTime, adjustFormat } from 'util/DateTime';

type Props = {
  dateTime?: string | number | Date | Moment,
  format?: DateTimeFormats,
};

/**
 * This component receives any date time and displays it in the browser time zone.
 */
const BrowserTime = ({ dateTime: dateTimeProp, format }: Props) => {
  const dateTime = dateTimeProp ?? new Date();
  const dateTimeString = adjustFormat(dateTime, 'internal');
  const timeInBrowserTimeZone = formatAsBrowserTime(dateTime, format);

  return (
    <time dateTime={dateTimeString} title={dateTimeString}>
      {timeInBrowserTimeZone}
    </time>
  );
};

BrowserTime.propTypes = {
  /**
   * Date time to be displayed in the component. You can provide an ISO
   * 8601 string, a JS native `Date` object, a moment `Date` object, or
   * a number containing seconds after UNIX epoch.
   */
  dateTime: PropTypes.oneOfType([PropTypes.string, PropTypes.object, PropTypes.number]),
  /**
   * Format to use to represent the date time.
   */
  format: PropTypes.string,
};

BrowserTime.defaultProps = {
  format: 'default',
  dateTime: undefined,
};

export default BrowserTime;
