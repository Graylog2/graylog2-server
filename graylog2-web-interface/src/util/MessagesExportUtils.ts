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
import { fetchFile } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';
import type { TimeRange } from 'views/logic/queries/Query';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { createLinkAndDownload } from 'util/FileDownloadUtils';
import type { QueryString } from 'views/logic/queries/types';

export type ExportPayload = {
  timerange?: TimeRange | undefined | null;
  query_string?: QueryString;
  streams?: string[];
  fields_in_order: string[] | undefined | null;
  execution_state?: SearchExecutionState;
  limit?: number;
};

const downloadFile = (exportJobId: string, filename: string) => {
  const href = qualifyUrl(ApiRoutes.MessagesController.jobResults(exportJobId, filename).url);
  createLinkAndDownload(href, filename);
};

export const exportSearchMessages = (
  exportPayload: ExportPayload,
  searchId: string,
  mimeType: string,
  filename?: string,
) => {
  const { url } = ApiRoutes.ExportJobsController.exportSearch(searchId);

  return fetchFile('POST', qualifyUrl(url), exportPayload, mimeType)
    .then((result: string) => downloadFile(result, filename))
    .catch(() => {
      UserNotification.error('Export failed');
    });
};

export const exportSearchTypeMessages = (
  exportPayload: ExportPayload,
  searchId: string,
  searchTypeId: string,
  mimeType: string,
  filename?: string,
) => {
  const { url } = ApiRoutes.ExportJobsController.exportSearchType(searchId, searchTypeId);

  return fetchFile('POST', qualifyUrl(url), exportPayload, mimeType)
    .then((result: string) => downloadFile(result, filename))
    .catch(() => {
      UserNotification.error('Export for widget failed');
    });
};
