// @flow strict
import download from 'downloadjs';

import { fetchFile } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';

import { type QueryString, type TimeRange } from 'views/logic/queries/Query';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';

export type ExportPayload = {
  timerange?: ?TimeRange,
  query_string?: QueryString,
  streams?: string[],
  fields_in_order: ?string[],
  sort: MessageSortConfig[]
}

const downloadCSV = (fileContent: string, fileName: string = 'search-result.csv') => {
  download(fileContent, fileName, 'text/csv');
};

export const exportAllMessages = (exportPayload: ExportPayload) => {
  const { url } = ApiRoutes.MessagesController.exportAll();
  fetchFile('POST', qualifyUrl(url), exportPayload)
    .then((result: string) => downloadCSV(result))
    .catch(() => {
      UserNotification.error('CSV Export failed');
    });
};

export const exportSearchTypeMessages = (exportPayload: ExportPayload, searchId: string, searchTypeId: string) => {
  const { url } = ApiRoutes.MessagesController.exportSearchType(searchId, searchTypeId);
  fetchFile('POST', qualifyUrl(url), exportPayload)
    .then((result: string) => downloadCSV(result))
    .catch(() => {
      UserNotification.error('CSV Export for widget failed');
    });
};
