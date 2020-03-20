// @flow strict
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';

import { type QueryString, type TimeRange } from 'views/logic/queries/Query';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';

export type ExportPayload = {
  timerange?: ?TimeRange,
  query_string?: QueryString,
  streams?: string[],
  fields_in_order: ?string[],
  sort: MessageSortConfig[]
}

export const exportAllMessages = (exportPayload: ExportPayload) => {
  const { url } = ApiRoutes.MessagesController.exportAll();
  fetch('POST', qualifyUrl(url), JSON.stringify(exportPayload))
    .catch(() => { UserNotification.error('CSV Export failed'); });
};

export const exportSearchTypeMessages = (exportPayload: ExportPayload, searchId: string, searchTypeId: string) => {
  const { url } = ApiRoutes.MessagesController.exportSearchType(searchId, searchTypeId);
  fetch('POST', qualifyUrl(url), JSON.stringify(exportPayload))
    .catch(() => { UserNotification.error('CSV Export for widget failed'); });
};
