// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled from 'styled-components';

import { LinkContainer } from 'components/graylog/router';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';
import { Button } from 'components/graylog';
import { IfPermitted, Spinner } from 'components/common';

type Props = {
  readOnly: $PropertyType<Role, 'readOnly'>,
  roleId: $PropertyType<Role, 'id'>,
  roleName: $PropertyType<Role, 'name'>,
};

const ActionsWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const _deleteRole = (roleId: $PropertyType<Role, 'id'>, roleName: $PropertyType<Role, 'name'>, setDeleting: boolean => void) => {
  let confirmMessage = `Do you really want to delete role "${roleName}"?`;
  const getOneUser = { page: 1, perPage: 1, query: '' };
  setDeleting(true);

  AuthzRolesDomain.loadUsersForRole(roleId, roleName, getOneUser).then((paginatedUsers) => {
    if (paginatedUsers.pagination.total >= 1) {
      confirmMessage += `\n\nIt is still assigned to ${paginatedUsers.pagination.total} users.`;
    }

    // eslint-disable-next-line no-alert
    if (window.confirm(confirmMessage)) {
      AuthzRolesDomain.delete(roleId, roleName).then(() => {
        setDeleting(false);
      });
    } else {
      setDeleting(false);
    }
  });
};

const ActionsCell = ({ roleId, roleName, readOnly }: Props) => {
  const [deleting, setDeleting] = useState(false);

  return (
    <td>
      <ActionsWrapper>
        <IfPermitted permissions={[`roles:edit:${roleName}`]}>
          <LinkContainer to={Routes.SYSTEM.AUTHZROLES.edit(encodeURIComponent(roleId))}>
            <Button id={`edit-role-${roleId}`} bsStyle="info" bsSize="xs" title={`Edit role ${roleName}`} type="button">
              Edit
            </Button>
          </LinkContainer>
        </IfPermitted>
        {!readOnly && (
          <IfPermitted permissions={[`roles:delete:${roleName}`]}>
            &nbsp;
            <Button id={`delete-role-${roleId}`} bsStyle="danger" bsSize="xs" title={`Delete role ${roleName}`} onClick={() => _deleteRole(roleId, roleName, setDeleting)} type="button">
              {deleting ? <Spinner text="Deleting" delay={0} /> : 'Delete'}
            </Button>
          </IfPermitted>
        )}
      </ActionsWrapper>
    </td>
  );
};

export default ActionsCell;
