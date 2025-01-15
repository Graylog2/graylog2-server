import { useQuery } from '@tanstack/react-query';

import { SystemContentPacks } from '@graylog/server-api';

import ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';
import { onError } from 'util/conditional/onError';
import UserNotification from 'util/UserNotification';

const fetchContentPackRevisions = async (id: string) => {
  const response = await SystemContentPacks.listContentPackRevisions(id);
  const contentPackRevision = new ContentPackRevisions(response.content_pack_revisions);
  const constraints = response.constraints_result;

  return {
    contentPackRevisions: contentPackRevision,
    selectedVersion: contentPackRevision.latestRevision,
    constraints: constraints,
  };
};

const defaultErrorHandler = (error: Error) => UserNotification.error(`Error while fetching content pack revisions: ${error}`, 'Unable to fetch content pack');

const useContentPackRevisions = (id: string, onFetchError: (e: Error) => void = defaultErrorHandler) => useQuery(
  ['content-packs', 'revisions', id],
  () => onError(fetchContentPackRevisions(id), onFetchError),
);
export default useContentPackRevisions;
