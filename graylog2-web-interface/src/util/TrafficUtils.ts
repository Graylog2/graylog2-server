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
import _ from 'lodash';
import moment from 'moment';
import crossfilter from 'crossfilter';

type Traffic = {
   [key: string]: number,
}

export const formatTrafficData = (traffic: Traffic) => {
  const ndx = crossfilter(_.map(traffic, (value, key) => ({ ts: key, bytes: value })));
  const dailyTraffic = ndx.dimension((d) => moment(d.ts).format('YYYY-MM-DD'));

  const dailySums = dailyTraffic.group().reduceSum((d) => d.bytes);
  const t = _.mapKeys(dailySums.all(), (entry) => moment.utc(entry.key, 'YYYY-MM-DD').toISOString());

  return _.mapValues(t, (val) => val.value);
};

export default {
  formatTrafficData,
};
