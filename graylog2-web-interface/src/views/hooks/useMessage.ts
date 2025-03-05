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

import type { Message } from 'views/components/messagelist/Types';
import { Messages } from '@graylog/server-api';
import MessageFormatter from 'logic/message/MessageFormatter';
import UserNotification from 'preflight/util/UserNotification';
import type FetchError from 'logic/errors/FetchError';

export const fetchMessage = async (index: string, id: string) => {
  const message = await Messages.search(index, id);

  return MessageFormatter.formatResultMessage(message);
};

const useMessage = (index: string, id: string, enabled = true): { data: Message | undefined, isInitialLoading: boolean; error?: FetchError; isError?: boolean } => {
  const { data, isInitialLoading, error, isError } = useQuery({
    queryKey: ['messages', index, id],
    retry: (count, e: FetchError) => {
      if (count > 3) {
        return false;
      }

      return e.status !== 404;
    },
    queryFn: () => fetchMessage(index, id),
    onError: (error) => {
      UserNotification.error(`Loading message information failed with status: ${error}`,
        'Could not load message information');
    },
    enabled,
  });

  return { data, isInitialLoading, error, isError };
};

export default useMessage;
