// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import UsersDomain from 'domainActions/users/UsersDomain';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { ErrorAlert } from 'components/common';
import User from 'logic/users/User';
import PaginatedItemOverview, {
  DEFAULT_PAGINATION,
  type DescriptiveItem,
} from 'components/common/PaginatedItemOverview';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import SectionComponent from 'components/common/Section/SectionComponent';
import RolesSelector from 'components/permissions/RolesSelector';

import RolesQueryHelp from '../RolesQueryHelp';

type Props = {
  user: User,
  onSubmit: ({ roles: string[] }) => Promise<void>,
};
const Container = styled.div`
  margin-top 15px;
  margin-bottom 15px;
`;

const RolesSection = ({ user, onSubmit }: Props) => {
  const { username, id } = user;
  const [loading, setLoading] = useState(false);
  const [paginatedRoles, setPaginatedRoles] = useState<?PaginatedRoles>();
  const [errors, setErrors] = useState();

  const _onLoad = useCallback((pagination = DEFAULT_PAGINATION) => {
    setLoading(true);

    return AuthzRolesDomain.loadRolesForUser(username, pagination).then((newPaginatedRoles) => {
      setLoading(false);

      return newPaginatedRoles;
    });
  }, [username]);

  const onRolesUpdate = (data: { roles: Array<string> }) => onSubmit(data).then(() => {
    _onLoad().then(setPaginatedRoles);
    UsersDomain.load(id);
  });

  const _onAssignRole = (newRoles: Immutable.Set<DescriptiveItem>) => {
    const userRoles = user.roles;
    const newRoleNames = newRoles.map((r) => r.name);
    const newUserRoles = userRoles.union(newRoleNames).toJS();

    setErrors();

    return onRolesUpdate({ roles: newUserRoles });
  };

  const ensureReaderOrAdminRole = (newRoles) => {
    return newRoles.includes('Reader') || newRoles.includes('Admin');
  };

  const onDeleteRole = (role: DescriptiveItem) => {
    const newUserRoles = Immutable.Set(user.roles.toJS()).remove(role.name).toJS();

    if (ensureReaderOrAdminRole(newUserRoles)) {
      onRolesUpdate({ roles: newUserRoles });
      setErrors();
    } else {
      setErrors('Roles must at least contain Admin or Reader role.');
      _onLoad().then(setPaginatedRoles);
    }
  };

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <h3>Assign Roles</h3>
      <Container>
        { /* $FlowFixMe: assignRole has DescriptiveItem */}
        <RolesSelector onSubmit={_onAssignRole} assignedRolesIds={user.roles} identifier={(role) => role.name} />
      </Container>

      <ErrorAlert onClose={setErrors}>
        {errors}
      </ErrorAlert>
      <h3>Selected Roles</h3>
      <Container>
        {/* $FlowFixMe Role is a DescriptiveItem! */}
        <PaginatedItemOverview noDataText="No selected roles have been found."
                               /* $FlowFixMe Role is a DescriptiveItem! */
                               onLoad={_onLoad}
                               /* $FlowFixMe Role is a DescriptiveItem! */
                               overrideList={paginatedRoles}
                               onDeleteItem={onDeleteRole}
                               queryHelper={<RolesQueryHelp />} />
      </Container>
    </SectionComponent>
  );
};

export default RolesSection;
