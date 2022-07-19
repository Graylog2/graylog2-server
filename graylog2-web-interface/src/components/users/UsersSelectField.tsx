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
import { useEffect, useState, useCallback } from 'react';
import debounce from 'lodash/debounce';
import type { PaginatedUsers } from 'src/stores/users/UsersStore';

import UsersDomain from 'domainActions/users/UsersDomain';
import { isPermitted } from 'util/PermissionsMixin';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { Spinner } from 'components/common';
import { useStore } from 'stores/connect';

import PaginatedSelect from '../common/Select/PaginatedSelect';

const DEFAULT_PAGINATION = { page: 1, perPage: 50, query: '', total: 0 };

const formatUsers = (users) => {
  return users.map((user) => ({ label: `${user.username} (${user.fullName})`, value: user.username }));
};

type Props = {
    value: string,
    onChange: (nextValue) => string,
}

const UsersSelectField = ({ value, onChange }: Props) => {
  const currentUser = useStore(CurrentUserStore, (state) => state?.currentUser);
  const [paginatedUsers, setPaginatedUsers] = useState<PaginatedUsers | undefined>();
  const loadUsersPaginated = useCallback((pagination = DEFAULT_PAGINATION) => {
    if (isPermitted(currentUser.permissions, 'users:list')) {
      return UsersDomain.loadUsersPaginated(pagination).then((newPaginatedUser) => {
        return newPaginatedUser;
      });
    }

    return undefined;
  }, [currentUser.permissions]);

  const loadMoreOptions = debounce(() => {
    const { pagination } = paginatedUsers;

    loadUsersPaginated({ ...pagination, page: pagination.page + 1 }).then((response) => {
      setPaginatedUsers((prevUsers) => {
        const list = prevUsers.list.concat(response.list);
        const newPagination = { ...prevUsers.pagination, ...response.pagination };

        return { ...prevUsers, list, pagination: newPagination } as PaginatedUsers;
      });
    });
  }, 200);

  useEffect(() => {
    loadUsersPaginated().then(setPaginatedUsers);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = debounce((newValue, actionMeta) => {
    if ((actionMeta.action === 'input-change')) {
      loadUsersPaginated({ ...DEFAULT_PAGINATION, query: newValue }).then(setPaginatedUsers);
    } else {
      loadUsersPaginated().then(setPaginatedUsers);
    }
  }, 200);

  if (!paginatedUsers) {
    return <p><Spinner text="Loading Notification information..." /></p>;
  }

  const { list, pagination: { total } } = paginatedUsers;

  return (
    <PaginatedSelect id="notification-user-recipients"
                     value={value}
                     placeholder="Select user(s)..."
                     options={formatUsers(list.toArray())}
                     onInputChange={handleSearch}
                     loadOptions={loadMoreOptions}
                     multi
                     total={total}
                     onChange={onChange} />
  );
};

UsersSelectField.propTypes = {};

export default UsersSelectField;
