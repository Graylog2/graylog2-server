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

// Type definitions for moment-precise-range 0.2
// Original:
// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/bc1eb3b28e41b2ed54865aecf134eae6c0b23add/types/moment-precise-range-plugin/index.d.ts
// Project: https://github.com/codebox/moment-precise-range
// Definitions by: Mitchell Grice <https://github.com/gricey432>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

import moment = require('moment');

export = moment;

declare module 'moment' {
  interface PreciseRangeValueObject {
    years: number;
    months: number;
    days: number;
    hours: number;
    minutes: number;
    seconds: number;
    firstDateWasLater: boolean;
  }

  interface Moment {
    preciseDiff(d2: Moment, returnValueObject?: false): string;
    preciseDiff(d2: Moment, returnValueObject: true): PreciseRangeValueObject;
  }

  function preciseDiff(d1: Moment, d2: Moment, returnValueObject?: false): string;
  function preciseDiff(d1: Moment, d2: Moment, returnValueObject: true): PreciseRangeValueObject;
}
