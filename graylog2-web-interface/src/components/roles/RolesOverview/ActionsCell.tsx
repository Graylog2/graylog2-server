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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import { LinkContainer, IfPermitted, Spinner } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import Routes from 'routing/Routes';
import type Role from 'logic/roles/Role';
import { Button } from 'components/bootstrap';
import i18n from 'i18n';

type Props = {
  readOnly: Role['readOnly'];
  roleId: Role['id'];
  roleName: Role['name'];
  onDeleted?: () => void;
};

const ActionsWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const _deleteRole = (
  roleId: Role['id'],
  roleName: Role['name'],
  setDeleting: (deleting: boolean) => void,
  onDeleted?: () => void,
) => {
  let confirmMessage = i18n.t('roles.actions.confirm_delete', { ns: 'users', name: roleName });
  const getOneUser = { page: 1, perPage: 1, query: '' };
  setDeleting(true);

  AuthzRolesDomain.loadUsersForRole(roleId, roleName, getOneUser).then((paginatedUsers) => {
    if (paginatedUsers.pagination.total >= 1) {
      confirmMessage += `\n\n${i18n.t('roles.actions.still_assigned', { ns: 'users', count: paginatedUsers.pagination.total })}`;
    }

    // eslint-disable-next-line no-alert
    if (window.confirm(confirmMessage)) {
      AuthzRolesDomain.delete(roleId, roleName).then(() => {
        setDeleting(false);

        if (onDeleted) onDeleted();
      });
    } else {
      setDeleting(false);
    }
  });
};

const ActionsCell = ({ roleId, roleName, readOnly, onDeleted }: Props) => {
  const { t } = useTranslation('users');
  const [deleting, setDeleting] = useState(false);

  return (
    <td>
      <ActionsWrapper>
        <IfPermitted permissions={[`roles:edit:${roleName}`]}>
          <LinkContainer to={Routes.SYSTEM.AUTHZROLES.edit(encodeURIComponent(roleId))}>
            <Button
              id={`edit-role-${roleId}`}
              bsSize="xs"
              title={t('roles.actions.edit_role_title', { name: roleName })}
              type="button">
              {t('roles.actions.edit')}
            </Button>
          </LinkContainer>
        </IfPermitted>
        {!readOnly && (
          <IfPermitted permissions={[`roles:delete:${roleName}`]}>
            <>
              &nbsp;
              <Button
                id={`delete-role-${roleId}`}
                bsStyle="danger"
                bsSize="xs"
                title={t('roles.actions.delete_role_title', { name: roleName })}
                onClick={() => _deleteRole(roleId, roleName, setDeleting, onDeleted)}
                type="button">
                {deleting ? <Spinner text={t('roles.actions.deleting')} delay={0} /> : t('roles.actions.delete')}
              </Button>
            </>
          </IfPermitted>
        )}
      </ActionsWrapper>
    </td>
  );
};

export default ActionsCell;
