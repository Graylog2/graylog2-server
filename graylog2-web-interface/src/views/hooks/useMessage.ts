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

import { Messages } from '@graylog/server-api';

import type { Message } from 'views/components/messagelist/Types';
import MessageFormatter from 'logic/message/MessageFormatter';
import { defaultOnError } from 'util/conditional/onError';

export const fetchMessage = async (index: string, id: string) => {
  const message = await Messages.search(index, id);

  return MessageFormatter.formatResultMessage(message);
};

const useMessage = (index: string, id: string, enabled = true): { data: Message | undefined, isInitialLoading: boolean } => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['messages', index, id],
    queryFn: () => defaultOnError(fetchMessage(index, id), 'Loading message information failed with status', 'Could not load message information'),
    enabled,
  });

  return { data, isInitialLoading };
};

export default useMessage;
