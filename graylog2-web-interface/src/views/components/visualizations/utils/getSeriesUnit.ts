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
import type Series from 'views/logic/aggregationbuilder/Series';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';

const getSeriesUnit = (series: Array<Series>, seriesName: string, units: UnitsConfig) => {
  const func = series.find((s) => s.config.name === seriesName || s.function === seriesName).function;
  const { field } = parseSeries(func);

  return units.getFieldUnit(field);
};

export default getSeriesUnit;
