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
// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';
import styled from 'styled-components';
import * as Immutable from 'immutable';

import { ErrorAlert, PaginatedItemOverview } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import UserOverview from 'logic/users/UserOverview';
import { DEFAULT_PAGINATION } from 'components/common/PaginatedItemOverview';
import SectionComponent from 'components/common/Section/SectionComponent';
import Role from 'logic/roles/Role';
import { PaginatedList } from 'stores/PaginationTypes';

import UsersSelector from './UsersSelector';

type Props = {
  role: Role,
};

const Container = styled.div`
  margin-top: 15px;
  margin-bottom: 15px;
`;

const UsersSection = ({ role: { id, name }, role }: Props) => {
  const [loading, setLoading] = useState(false);
  const [paginatedUsers, setPaginatedUsers] = useState<PaginatedList<UserOverview>>();
  const [errors, setErrors] = useState<string | undefined>();

  const _onLoad = useCallback((pagination) => {
    setLoading(true);

    return AuthzRolesDomain.loadUsersForRole(id, name, pagination)
      .then((paginatedRoles) => {
        setLoading(false);

        return paginatedRoles;
      });
  }, [id, name]);

  const _onAssignUser = (newUsers: Immutable.Set<UserOverview>) => AuthzRolesDomain.addMembers(id,
    newUsers.map((u) => u.username).toSet()).then(() => _onLoad(DEFAULT_PAGINATION)
    .then((result) => {
      setPaginatedUsers(result);

      return result;
    }));

  const _onUnassignUser = (user) => {
    if ((role.name === 'Reader' || role.name === 'Admin')
      && (!user.roles.includes('Reader') || !user.roles.includes('Admin'))) {
      setErrors(`User '${user.name}' needs at least a Reader or Admin role.`);
      _onLoad(DEFAULT_PAGINATION).then(setPaginatedUsers);

      return;
    }

    setErrors(undefined);

    AuthzRolesDomain.removeMember(id, user.name).then(() => {
      _onLoad(DEFAULT_PAGINATION).then(setPaginatedUsers);
    });
  };

  return (
    <SectionComponent title="Users" showLoading={loading}>
      <h3>Assign Users</h3>
      <Container>
        <UsersSelector onSubmit={_onAssignUser} role={role} />
      </Container>
      <ErrorAlert onClose={setErrors}>
        {errors}
      </ErrorAlert>
      <h3>Selected Users</h3>
      <Container>
        <PaginatedItemOverview noDataText="No selected users have been found."
                               onLoad={_onLoad}
                               overrideList={paginatedUsers}
                               onDeleteItem={_onUnassignUser} />
      </Container>
    </SectionComponent>
  );
};

export default UsersSection;
