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
import cronstrue from 'cronstrue';

// eslint-disable-next-line import/prefer-default-export
export function describeExpression(expression: string): string | null {
  if (expression) {
    // The false throwExceptionOnParseError option just returns the parse error text.
    // The backend cron library uses 1-based days of week instead of 0-based - Sunday equals 1 as opposed to 0.
    // The false dayOfWeekStartIndexZero option aligns the frontend library with the backend.
    return cronstrue.toString(expression, { throwExceptionOnParseError: false, dayOfWeekStartIndexZero: false });
  }

  return null;
}
