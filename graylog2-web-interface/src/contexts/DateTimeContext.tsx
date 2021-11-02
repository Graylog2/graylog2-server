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
import type { Moment } from 'moment';

import { singleton } from 'logic/singleton';

import { DATE_TIME_FORMATS } from './DateTimeProvider';

export type DateTimeFormats = keyof typeof DATE_TIME_FORMATS;

export type DateTime = string | number | Moment | Date;

export type DateTimeContextType = {
  relativeDifference: (time: DateTime, tz?: string) => string
  formatAsBrowserTime: (time: DateTime, format?: DateTimeFormats) => string,
  formatTime: (time: DateTime, tz?: string, format?: DateTimeFormats) => string
  adjustTimezone: (time: DateTime, tz?: string, format?: DateTimeFormats) => Moment,
  userTimezone: string,
};

const DateTimeContext = React.createContext<DateTimeContextType | undefined>(undefined);

export default singleton('contexts.TimeLocalizeContext', () => DateTimeContext);
