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

import type { DateTime } from 'util/DateTime';
import { adjustFormat, toDateObject } from 'util/DateTime';
import type { UserDateTimeContextType } from 'contexts/UserDateTimeContext';

const userTimezone = 'Europe/Berlin';
const userDateTimeContextValue: UserDateTimeContextType = {
  formatTime: (dateTime: DateTime) => adjustFormat(dateTime),
  toUserTimezone: (dateTime: DateTime) => toDateObject(dateTime, undefined, userTimezone),
  userTimezone,
};

const useUserDateTimeMock = () => userDateTimeContextValue;

export default useUserDateTimeMock;
