// @flow strict
import * as React from 'react';
import { useState } from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import UsersDomain from 'domainActions/users/UsersDomain';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import User from 'logic/users/User';
import PaginatedItemOverview, {
  defaultPageInfo,
  type PaginationInfo,
  type PaginatedListType,
  type DescriptiveItem,
} from 'components/common/PaginatedItemOverview';
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
  const { username } = user;
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState();

  const _onLoad = ({ page, perPage, query }: PaginationInfo = defaultPageInfo): Promise<?PaginatedListType> => {
    setLoading(true);

    return AuthzRolesDomain.loadRolesForUser(username, page, perPage, query)
      .then((response) => {
        setLoading(false);

        return {
          pagination: response.pagination,
          list: response.list.map((item) => (item: DescriptiveItem)),
        };
      });
  };

  const onRolesUpdate = (data: {roles: Array<string>}) => onSubmit(data).then(() => {
    _onLoad().then((response) => { if (response) setRoles(response); });
    UsersDomain.load(username);
  });

  const _onAssignRole = (newRoles: Immutable.Set<DescriptiveItem>) => {
    const userRoles = user.roles;
    const newRoleNames = newRoles.map((r) => r.name);
    const newUserRoles = userRoles.union(newRoleNames).toJS();

    return onRolesUpdate({ roles: newUserRoles });
  };

  const onDeleteRole = (role: DescriptiveItem) => {
    const newUserRoles = Immutable.Set(user.roles.toJS()).remove(role.name).toJS();

    onRolesUpdate({ roles: newUserRoles });
  };

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <h3>Assign Roles</h3>
      <Container>
        { /* $FlowFixMe: assignRole has DescriptiveItem */}
        <RolesSelector onSubmit={_onAssignRole} assignedRolesIds={user.roles} identifier={(role) => role.name} />
      </Container>
      <h3>Selected Roles</h3>
      <Container>
        <PaginatedItemOverview noDataText="No selected roles have been found."
                               onLoad={_onLoad}
                               overrideList={roles}
                               onDeleteItem={onDeleteRole}
                               queryHelper={<RolesQueryHelp />} />
      </Container>
    </SectionComponent>
  );
};

export default RolesSection;
