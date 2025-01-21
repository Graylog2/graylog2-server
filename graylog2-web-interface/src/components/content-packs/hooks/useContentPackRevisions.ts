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
