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

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';

type SortOrder = 'asc' | 'desc';

export function sortByDate(d1: string, d2: string, sortOrder: SortOrder = 'asc') {
  const d1Time = moment(d1);
  const d2Time = moment(d2);

  const [earlier, later] = sortOrder === 'asc' ? [d1Time, d2Time] : [d2Time, d1Time];

  if (earlier.isBefore(later)) {
    return -1;
  }

  if (later.isBefore(earlier)) {
    return 1;
  }

  return 0;
}

export function naturalSortIgnoreCase(s1: string, s2: string, sortOrder: SortOrder = 'asc') {
  return sortOrder === 'asc'
    ? naturalSort(s1.toLowerCase(), s2.toLowerCase())
    : naturalSort(s2.toLowerCase(), s1.toLowerCase());
}
