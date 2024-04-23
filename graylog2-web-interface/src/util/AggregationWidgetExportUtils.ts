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

import { fetchFileWithBlob } from 'util/FileDownloadUtils';
import { qualifyUrl } from 'util/URLUtils';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';

export type Extension = 'csv' | 'json';

export type Result = {
  total: number,
  rows: Rows,
  effective_timerange: AbsoluteTimeRange,
};

const mimeTypeMapper: Record<Extension, string> = {
  csv: 'text/csv',
  json: 'application/json',
};

const getUrl = (fileName: string) => qualifyUrl(`views/search/pivot/export/${fileName}`);

export const exportWidget = (widgetTitle: string, widgetResults: Result, extension: Extension) => {
  const fileName = `${widgetTitle}_${widgetResults.effective_timerange.from}-${widgetResults.effective_timerange.to}.${extension}`;
  const mimeType = mimeTypeMapper[extension];

  return fetchFileWithBlob('POST', getUrl(fileName), widgetResults, mimeType, fileName);
};
