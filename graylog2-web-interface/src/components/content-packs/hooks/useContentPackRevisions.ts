import { useQuery } from '@tanstack/react-query';

import { SystemContentPacks } from '@graylog/server-api';

import ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';
import { onError } from 'util/conditional/onError';

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

const useContentPackRevisions = (id: string, onFetchError: (e: Error) => void) => useQuery(
  ['content-packs', 'revisions', id],
  () => onError(fetchContentPackRevisions(id), onFetchError),
);
export default useContentPackRevisions;
