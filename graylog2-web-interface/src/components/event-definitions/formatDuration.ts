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
import moment from 'moment';

import i18n from 'i18n';

/** Formats a duration in the current language, e.g. "1 day 2 hours". Units which are zero are left out. */
const formatDuration = (durationInMs: number) => {
  const duration = moment.duration(durationInMs);

  return [
    { unit: 'days', count: Math.floor(duration.asDays()) },
    { unit: 'hours', count: duration.hours() },
    { unit: 'minutes', count: duration.minutes() },
    { unit: 'seconds', count: duration.seconds() },
  ]
    .filter(({ count }) => count > 0)
    .map(({ unit, count }) => i18n.t(`duration.${unit}`, { ns: 'common', count }))
    .join(' ');
};

export default formatDuration;
