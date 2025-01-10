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
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { ContentStream } from '@graylog/server-api';

import UserNotification from 'preflight/util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';
import { CONTENT_STREAM_CONTENT_KEY } from 'components/content-stream/hook/useContentStream';
import { defaultOnError } from 'util/conditional/onError';

export const CONTENT_STREAM_SETTINGS_KEY = ['content-stream', 'settings'];
export const CONTENT_STREAM_TAGS_KEY = ['content-stream', 'tags'];

type ContentStreamSettingsApi = {
  content_stream_topics: Array<string>;
  releases_enabled: boolean;
  content_stream_enabled: boolean;
};

type ContentStreamSettings = {
  contentStreamTopics: Array<string>;
  releasesSectionEnabled: boolean;
  contentStreamEnabled: boolean;
};

const useContentStreamSettings = (): {
  contentStreamSettings: ContentStreamSettings,
  isLoadingContentStreamSettings: boolean,
  onSaveContentStreamSetting: ({ settings, username }: {
    settings: ContentStreamSettingsApi,
    username: string,
  }) => Promise<void>,
  contenStreamTags: {
    currentTag: string,
    isLoadingTags: boolean,
    refetchContentStreamTag: () => void,
    contentStreamTagError: Error,
  },
  refetchContentStream: () => void,
} => {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const { getContentStreamUserSettings, setContentStreamUserSettings, getContentStreamTags } = ContentStream;

  const saveSettings = async ({ settings, username }: { settings: ContentStreamSettingsApi, username: string }) => {
    await setContentStreamUserSettings(settings, username);
  };

  const {
    data,
    isLoading,
    refetch: refetchContentStream,
  } = useQuery<ContentStreamSettingsApi, Error>(
    [CONTENT_STREAM_SETTINGS_KEY],
    () => defaultOnError(getContentStreamUserSettings(currentUser.username), 'Loading content stream config failed with status', 'Could not load content stream.'),
  );
  const {
    data: tags,
    isLoading: isLoadingTags,
    refetch: refetchContentStreamTag,
    error: contentStreamTagError,
  } = useQuery<Array<string>, Error>(
    [CONTENT_STREAM_TAGS_KEY],
    () => defaultOnError(getContentStreamTags(), 'Loading content stream tag failed with status', 'Could not load content stream tags.'),
  );
  const { mutateAsync: onSaveContentStreamSetting } = useMutation(saveSettings, {
    onSuccess: () => {
      queryClient.invalidateQueries(CONTENT_STREAM_SETTINGS_KEY);
      queryClient.invalidateQueries(CONTENT_STREAM_CONTENT_KEY);
    },
    onError: (errorThrown) => {
      UserNotification.error(`Enabling content stream failed with status: ${errorThrown}`,
        'Could not cancel instant archiving jobs');
    },
  });

  return {
    contentStreamSettings: {
      contentStreamTopics: data?.content_stream_topics,
      releasesSectionEnabled: data?.releases_enabled,
      contentStreamEnabled: data?.content_stream_enabled,
    },
    contenStreamTags: {
      currentTag: tags?.[0],
      isLoadingTags,
      refetchContentStreamTag,
      contentStreamTagError,
    },
    isLoadingContentStreamSettings: isLoading,
    refetchContentStream,
    onSaveContentStreamSetting,
  };
};

export default useContentStreamSettings;
