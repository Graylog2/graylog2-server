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

import { MessagesActions } from 'stores/messages/MessagesStore';
import type { Message } from 'views/components/messagelist/Types';

const useMessage = (id: string, index: string): { data: Message | undefined, isInitialLoading: boolean } => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['messages', index, id],
    queryFn: () => MessagesActions.loadMessage(index, id),
  });

  return { data, isInitialLoading };
};

export default useMessage;
