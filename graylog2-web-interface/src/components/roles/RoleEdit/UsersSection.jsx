// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';
import styled from 'styled-components';
import * as Immutable from 'immutable';

import { Alert, Button } from 'components/graylog';
import { Icon, PaginatedItemOverview } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import UserOverview from 'logic/users/UserOverview';
import { DEFAULT_PAGINATION } from 'components/common/PaginatedItemOverview';
import SectionComponent from 'components/common/Section/SectionComponent';
import Role from 'logic/roles/Role';

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
  const [paginatedUsers, setPaginatedUsers] = useState();
  const [errors, setErrors] = useState();

  const _onLoad = useCallback((pagination) => {
    setLoading(true);

    return AuthzRolesDomain.loadUsersForRole(id, name, pagination)
      .then((paginatedRoles) => {
        setLoading(false);

        return paginatedRoles;
      });
  }, [id, name]);

  const _onAssignUser = (newUsers: Immutable.Set<UserOverview>) => AuthzRolesDomain.addMembers(id,
    newUsers.map((u) => u.username)).then(() => _onLoad(DEFAULT_PAGINATION)
    .then(setPaginatedUsers));

  const _onUnassignUser = (user) => {
    if ((role.name === 'Reader' || role.name === 'Admin')
      && (!user.roles.includes('Reader') || !user.roles.includes('Admin'))) {
      setErrors(`User '${user.name}' needs at least a Reader or Admin role.`);
      _onLoad(DEFAULT_PAGINATION).then(setPaginatedUsers);

      return;
    }

    setErrors();

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
      { errors && (
        <Container>
          <Alert bsStyle="warning">
            {errors}
            <Button bsSize="xsmall" bsStyle="warning" className="pull-right" onClick={() => setErrors()}>
              <Icon name="times" />
            </Button>
          </Alert>
        </Container>
      )}
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
