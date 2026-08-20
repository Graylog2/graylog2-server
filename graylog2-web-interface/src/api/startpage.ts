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
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

export const setStartpage = (userId: string, type: string, id: string): Promise<void> => {
  const url = qualifyUrl(ApiRoutes.UsersApiController.update(userId).url);
  const payload: { type?: string; id?: string } = {};

  if (type && id) {
    payload.type = type;
    payload.id = id;
  }

  return fetch('PUT', url, { startpage: payload }).then(
    () => {
      CurrentUserStore.reload();
      UserNotification.success('Your start page was changed successfully');
    },
    (error: unknown) =>
      UserNotification.error(
        `Changing your start page failed with error: ${error}`,
        'Could not change your start page',
      ),
  );
};
