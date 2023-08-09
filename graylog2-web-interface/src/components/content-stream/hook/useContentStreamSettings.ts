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
/*
import { useQuery } from '@tanstack/react-query';

import { ContentStream } from '@graylog/server-api';
import UserNotification from 'preflight/util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

export const CONTENT_STREAM_SETTINGS_KEY = 'CONTENT_STREAM_SETTINGS';

const useContentStream = () => {
  const { data, isLoading } = useQuery([CONTENT_STREAM_SETTINGS], () => {
  }, {
    onError: (errorThrown) => {
      UserNotification.error(`Loading news feed failed with status: ${errorThrown}`,
        'Could not load news feed');
    },
    initialData: [],
  });

  return {
    newsList: data,
    isLoadingFeed: isLoading,
  };
};

export default useContentStream;
*/
