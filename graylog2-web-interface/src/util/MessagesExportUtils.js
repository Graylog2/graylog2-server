// @flow strict
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

import { type QueryString, type TimeRange } from 'views/logic/queries/Query';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';

const MESSAGES_EXPORT_PATH = 'views/search/messages';

type ExportPayload = {
  timerange: TimeRange,
  query_string: QueryString,
  streams: ?string[],
  fields_in_order: ?string[],
  sort: MessageSortConfig[]
}

export const exportAllMessages = (exportPayload: ExportPayload) => {
  fetch('POST', `${MESSAGES_EXPORT_PATH}`, JSON.stringify(exportPayload))
    .catch(() => { UserNotification.error('CSV Export failed'); });
};

export const exportSearchTypeMessages = (exportPayload: ExportPayload, searchId: string, searchTypeId: string) => {
  fetch('POST', `${MESSAGES_EXPORT_PATH}/${searchId}/${searchTypeId}`, JSON.stringify(exportPayload))
    .catch(() => { UserNotification.error('CSV Export for widget failed'); });
};
