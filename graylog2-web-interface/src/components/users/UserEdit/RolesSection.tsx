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
import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import * as Immutable from 'immutable';
import styled from 'styled-components';
import { useQueryClient } from '@tanstack/react-query';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { USERS_QUERY_KEY } from 'hooks/useUsers';
import { ErrorAlert } from 'components/common';
import type User from 'logic/users/User';
import type { DescriptiveItem } from 'components/common/PaginatedItemOverview';
import PaginatedItemOverview, { DEFAULT_PAGINATION } from 'components/common/PaginatedItemOverview';
import type { PaginatedRoles } from 'hooks/useAuthzRoles';
import SectionComponent from 'components/common/Section/SectionComponent';
import RolesSelector from 'components/permissions/RolesSelector';
import RolesQueryHelper from 'components/roles/RolesQueryHelper';

type Props = {
  user: User;
  onSubmit: (payload: { roles: string[] }) => Promise<void>;
};

const Container = styled.div`
  margin-top: 15px;
  margin-bottom: 15px;
`;

const RolesSection = ({ user, onSubmit }: Props) => {
  const { t } = useTranslation('users');
  const { username, id } = user;
  const queryClient = useQueryClient();
  const [loading, setLoading] = useState(false);
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined>();
  const [errors, setErrors] = useState<string | undefined>();

  const _onLoad = useCallback(
    (pagination = DEFAULT_PAGINATION) => {
      setLoading(true);

      return AuthzRolesDomain.loadRolesForUser(username, pagination).then((newPaginatedRoles) => {
        setLoading(false);

        return newPaginatedRoles;
      });
    },
    [username],
  );

  const onRolesUpdate = (data: { roles: Array<string> }) =>
    onSubmit(data).then(() => {
      _onLoad().then(setPaginatedRoles);
      queryClient.invalidateQueries({ queryKey: [...USERS_QUERY_KEY, id] });
    });

  const _onAssignRole = (newRoles: Immutable.Set<DescriptiveItem>) => {
    const userRoles = user.roles;
    const newRoleNames = newRoles.map((r) => r.name);
    const newUserRoles = userRoles.union(newRoleNames).toJS();

    setErrors(undefined);

    return onRolesUpdate({ roles: newUserRoles });
  };

  const ensureReaderOrAdminRole = (newRoles: Array<string>) =>
    newRoles.includes('Reader') || newRoles.includes('Admin');

  const onDeleteRole = (role: DescriptiveItem) => {
    const newUserRoles = Immutable.Set(user.roles.toJS()).remove(role.name).toJS();

    if (ensureReaderOrAdminRole(newUserRoles)) {
      onRolesUpdate({ roles: newUserRoles });
      setErrors(undefined);
    } else {
      setErrors(t('user_edit.roles_required'));
      _onLoad().then(setPaginatedRoles);
    }
  };

  return (
    <SectionComponent title={t('user_fields.roles')} showLoading={loading}>
      <h3>{t('user_fields.assign_roles')}</h3>
      <Container>
        <RolesSelector onSubmit={_onAssignRole} assignedRolesIds={user.roles} identifier={(role) => role.name} />
      </Container>

      <ErrorAlert onClose={setErrors}>{errors}</ErrorAlert>
      <h3>{t('user_fields.selected_roles')}</h3>
      <Container>
        <PaginatedItemOverview
          noDataText={t('user_details.roles_none_selected')}
          onLoad={_onLoad}
          overrideList={paginatedRoles}
          onDeleteItem={onDeleteRole}
          queryHelper={<RolesQueryHelper />}
        />
      </Container>
    </SectionComponent>
  );
};

export default RolesSection;
