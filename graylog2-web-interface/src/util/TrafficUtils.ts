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
import map from 'lodash/map';
import mapKeys from 'lodash/mapKeys';
import mapValues from 'lodash/mapValues';
import moment from 'moment';
import crossfilter from 'crossfilter';

type Traffic = {
   [key: string]: number,
}

export const formatTrafficData = (traffic: Traffic) => {
  const ndx = crossfilter(map(traffic, (value, key) => ({ ts: key, bytes: value })));
  const dailyTraffic = ndx.dimension((d) => moment(d.ts).format('YYYY-MM-DD'));

  const dailySums = dailyTraffic.group().reduceSum((d) => d.bytes);
  const t = mapKeys(dailySums.all(), (entry) => moment.utc(entry.key, 'YYYY-MM-DD').toISOString());

  return mapValues(t, (val) => val.value);
};

export default {
  formatTrafficData,
};
