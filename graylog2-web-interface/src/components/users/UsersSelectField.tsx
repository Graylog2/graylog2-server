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
import * as React from 'react';
import { useCallback } from 'react';

import UsersDomain from 'domainActions/users/UsersDomain';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

import PaginatedSelect from '../common/Select/PaginatedSelect';

const formatUsers = (users) => users.map((user) => ({ label: `${user.username} (${user.fullName})`, value: user.username }));

type Props = {
  value: string,
  onChange: (nextValue) => void,
}

const UsersSelectField = ({ value, onChange }: Props) => {
  const currentUser = useCurrentUser();

  const loadUsers = useCallback((pagination: { page: number, perPage: number, query: string }) => {
    if (!isPermitted(currentUser.permissions, 'users:list')) {
      return Promise.resolve({
        pagination,
        total: 0,
        list: [],
      });
    }

    return UsersDomain.loadUsersPaginated(pagination).then((results) => ({
      total: results.pagination.total,
      list: formatUsers(results.list.toArray()),
      pagination,
    }));
  }, [currentUser.permissions]);

  return (
    <PaginatedSelect id="user-select-list"
                     value={value}
                     placeholder="Select user(s)..."
                     onLoadOptions={loadUsers}
                     multi
                     onChange={onChange} />
  );
};

export default UsersSelectField;
