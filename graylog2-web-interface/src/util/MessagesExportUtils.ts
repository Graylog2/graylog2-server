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
// @flow strict
import download from 'downloadjs';

import { fetchFile } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';
import { QueryString, TimeRange } from 'views/logic/queries/Query';

export type ExportPayload = {
  timerange?: TimeRange | undefined | null,
  // eslint-disable-next-line camelcase
  query_string?: QueryString,
  streams?: string[],
  // eslint-disable-next-line camelcase
  fields_in_order: string[] | undefined | null,
};

const downloadCSV = (fileContent: string, filename: string = 'search-result') => {
  download(fileContent, `${filename}.csv`, 'text/csv');
};

export const exportSearchMessages = (exportPayload: ExportPayload, searchId: string, filename?: string) => {
  const { url } = ApiRoutes.MessagesController.exportSearch(searchId);

  return fetchFile('POST', qualifyUrl(url), exportPayload)
    .then((result: string) => downloadCSV(result, filename))
    .catch(() => {
      UserNotification.error('CSV Export failed');
    });
};

export const exportSearchTypeMessages = (exportPayload: ExportPayload, searchId: string, searchTypeId: string, filename?: string) => {
  const { url } = ApiRoutes.MessagesController.exportSearchType(searchId, searchTypeId, filename);

  return fetchFile('POST', qualifyUrl(url), exportPayload)
    .then((result: string) => downloadCSV(result, filename))
    .catch(() => {
      UserNotification.error('CSV Export for widget failed');
    });
};
