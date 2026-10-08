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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { Users } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

const deleteToken = async (userId: string, tokenId: string) => Users.revokeToken(userId, tokenId);

const useDeleteTokenMutation = (userId: string, tokenId: string) => {
  const queryClient = useQueryClient();

  const remove = useMutation({
    mutationFn: () => deleteToken(userId, tokenId),

    onError: (errorThrown) => {
      UserNotification.error(`Token deletion failed: ${errorThrown}`, 'Could not delete token');
    },

    onSuccess: () => {
      UserNotification.success('Token has been successfully deleted.', 'Success!');

      queryClient.invalidateQueries({
        queryKey: ['token-management', 'overview'],
      });
    },
  });

  return { deleteToken: remove.mutateAsync };
};

export default useDeleteTokenMutation;
