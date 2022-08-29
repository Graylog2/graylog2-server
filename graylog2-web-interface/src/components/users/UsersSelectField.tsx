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
import { useEffect, useState, useCallback, useContext } from 'react';
import debounce from 'lodash/debounce';
import type { PaginatedUsers } from 'src/stores/users/UsersStore';

import UsersDomain from 'domainActions/users/UsersDomain';
import { isPermitted } from 'util/PermissionsMixin';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { Spinner } from 'components/common';

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
  const currentUser = useContext(CurrentUserContext);
  const [paginatedUsers, setPaginatedUsers] = useState<PaginatedUsers | undefined>();
  const [isNextPageLoading, setIsNextPageLoading] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const loadUsersPaginated = useCallback((pagination = DEFAULT_PAGINATION) => {
    if (isPermitted(currentUser.permissions, 'users:list')) {
      setIsNextPageLoading(true);

      return UsersDomain.loadUsersPaginated(pagination).then((newPaginatedUser) => {
        setIsNextPageLoading(false);

        return newPaginatedUser;
      });
    }

    return undefined;
  }, [currentUser.permissions]);

  const loadUsers = (pagination, query = '') => {
    loadUsersPaginated({ ...pagination, page: pagination.page + 1, query }).then((response) => {
      setPaginatedUsers((prevUsers) => {
        const list = prevUsers.list.concat(response.list);
        const newPagination = { ...prevUsers.pagination, ...response.pagination };

        return { ...prevUsers, list, pagination: newPagination } as PaginatedUsers;
      });
    });
  };

  const loadMoreOptions = debounce(() => {
    const { pagination, pagination: { total }, list } = paginatedUsers;

    if (total > list.count()) {
      loadUsers(pagination);
    }
  }, 400);

  useEffect(() => {
    if (!paginatedUsers) {
      loadUsersPaginated().then(setPaginatedUsers);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = debounce((newValue, actionMeta) => {
    if ((actionMeta.action === 'input-change')) {
      setIsSearching(true);

      loadUsersPaginated({ ...DEFAULT_PAGINATION, query: newValue }).then((results) => {
        setIsSearching(true);
        setPaginatedUsers(results);
      });
    } else if (actionMeta.action === 'menu-close') {
      loadUsersPaginated().then(setPaginatedUsers);
    }
  }, 400);

  if (!paginatedUsers) {
    return <p><Spinner text="Loading User select..." /></p>;
  }

  const { list, pagination: { total } } = paginatedUsers;

  return (
    <PaginatedSelect id="user-select-list"
                     value={value}
                     placeholder="Select user(s)..."
                     options={formatUsers(list.toArray())}
                     onInputChange={handleSearch}
                     loadOptions={isNextPageLoading || isSearching ? () => {} : loadMoreOptions}
                     multi
                     total={total}
                     onChange={onChange} />
  );
};

UsersSelectField.propTypes = {};

export default UsersSelectField;
