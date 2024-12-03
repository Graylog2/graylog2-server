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

const getFieldNameFromTrace = ({ series, fullPath }: { series: Array<Series>, fullPath: string | undefined }) => {
  if (!fullPath) return null;

  const desireSeries = series.find((s) => {
    const nameToFInd = s.config.name ?? s.function;

    return fullPath.endsWith(nameToFInd);
  });

  if (!desireSeries) return null;

  return parseSeries(desireSeries.function).field;
};

export default getFieldNameFromTrace;
