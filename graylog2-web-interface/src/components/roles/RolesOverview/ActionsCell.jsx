// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { LinkContainer } from 'react-router-bootstrap';

import UserNotification from 'util/UserNotification';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';
import { Button } from 'components/graylog';
import { IfPermitted } from 'components/common';

type Props = {
  readOnly: $PropertyType<Role, 'readOnly'>,
  roleId: $PropertyType<Role, 'id'>,
  roleName: $PropertyType<Role, 'name'>,
};

const ActtionsWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const ActionsCell = ({ roleId, roleName, readOnly }: Props) => {
  const _deleteRole = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete role ${roleName}?`)) {
      AuthzRolesActions.deleteRole(roleId).then(() => {
        UserNotification.success(`Role "${roleName}" was deleted successfully`);
      }, () => {
        UserNotification.error(`There was an error deleting the role "${roleName}"`);
      });

      // AuthzRolesActions.getMembers(roleName).then((membership) => {
      //   if (membership.users.length !== 0) {
      //     UserNotification.error(`Cannot delete role ${roleName}. It is still assigned to ${membership.users.length} users.`);
      //   } else {
      //     AuthzRolesActions.deleteRole(roleId).then(() => {
      //       UserNotification.success(`Role "${roleName}" was deleted successfully`);
      //     }, () => {
      //       UserNotification.error(`There was an error deleting the role "${roleName}"`);
      //     });
      //   }
      // });
    }
  };

  return (
    <td>
      <ActtionsWrapper>
        <IfPermitted permissions={[`roles:edit:${roleName}`]}>
          <LinkContainer to={Routes.SYSTEM.ROLES.edit(encodeURIComponent(roleName))}>
            <Button id={`edit-role-${roleName}`} bsStyle="info" bsSize="xs" title={`Edit role ${roleName}`} type="button">
              Edit
            </Button>
          </LinkContainer>
        </IfPermitted>
        {!readOnly && (
          <IfPermitted permissions={[`roles:delete:${roleName}`]}>
            &nbsp;
            <Button id={`delete-role-${roleName}`} bsStyle="danger" bsSize="xs" title={`Delete role ${roleName}`} onClick={_deleteRole} type="button">
              Delete
            </Button>
          </IfPermitted>
        )}
      </ActtionsWrapper>
    </td>
  );
};

export default ActionsCell;
