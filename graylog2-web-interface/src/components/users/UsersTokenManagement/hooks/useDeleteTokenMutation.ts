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

import i18n from 'i18n';
import UserNotification from 'util/UserNotification';

const deleteToken = async (userId: string, tokenId: string) => Users.revokeToken(userId, tokenId);

const useDeleteTokenMutation = (userId: string, tokenId: string) => {
  const queryClient = useQueryClient();

  const remove = useMutation({
    mutationFn: () => deleteToken(userId, tokenId),

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('token_admin.delete_failed', { ns: 'users', error: errorThrown }),
        i18n.t('token_admin.delete_failed_title', { ns: 'users' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('token_admin.deleted', { ns: 'users' }),
        i18n.t('token_admin.deleted_title', { ns: 'users' }),
      );

      queryClient.invalidateQueries({
        queryKey: ['token-management', 'overview'],
      });
    },
  });

  return { deleteToken: remove.mutateAsync };
};

export default useDeleteTokenMutation;
