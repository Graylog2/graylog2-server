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
import naturalSort from 'javascript-natural-sort';

// sortOrder: "asc"|"desc"
export function sortByDate(d1, d2, sortOrder) {
  const effectiveSortOrder = sortOrder || 'asc';
  const d1Time = moment(d1);
  const d2Time = moment(d2);

  if (effectiveSortOrder === 'asc') {
    return (d1Time.isBefore(d2Time) ? -1 : d2Time.isBefore(d1Time) ? 1 : 0);
  }

  return (d2Time.isBefore(d1Time) ? -1 : d1Time.isBefore(d2Time) ? 1 : 0);
}

export function naturalSortIgnoreCase(s1, s2, sortOrder) {
  const effectiveSortOrder = sortOrder || 'asc';

  return (effectiveSortOrder === 'asc' ? naturalSort(s1.toLowerCase(), s2.toLowerCase()) : naturalSort(s2.toLowerCase(), s1.toLowerCase()));
}
