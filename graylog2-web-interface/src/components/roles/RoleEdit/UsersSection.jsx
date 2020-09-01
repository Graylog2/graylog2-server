// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled from 'styled-components';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import UserOverview from 'logic/users/UserOverview';
import {
  defaultPageInfo,
  type PaginationInfo,
  type PaginatedListType,
  type DescriptiveItem,
} from 'components/common/PaginatedItemOverview';
import { PaginatedItemOverview } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import Role from 'logic/roles/Role';

import UsersSelector from './UsersSelector';

type Props = {
  role: Role,
};

const Container = styled.div`
  margin-top 15px;
  margin-bottom 15px;
`;

const UsersSection = ({ role: { id, name }, role }: Props) => {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState();

  const _onLoad = ({ page, perPage, query }: PaginationInfo): Promise<?PaginatedListType> => {
    setLoading(true);

    return AuthzRolesDomain.loadUsersForRole(id, name, page, perPage, query)
      .then((response) => {
        setLoading(false);

        // $FlowFixMe UserOverview is a DescriptiveItem!!!
        return response;
      });
  };

  const _loadUsers = (u) => u && setUsers(u);
  const _onAssignUser = (user: UserOverview) => AuthzRolesDomain.addMember(id, user.username).then(() => _onLoad(defaultPageInfo)
    .then(_loadUsers));

  const _onUnassignUser = (user: DescriptiveItem) => {
    AuthzRolesDomain.removeMember(id, user.name).then(() => {
      _onLoad(defaultPageInfo).then(_loadUsers);
    });
  };

  return (
    <SectionComponent title="Users" showLoading={loading}>
      <h3>Assign Users</h3>
      <Container>
        <UsersSelector onSubmit={_onAssignUser} role={role} />
      </Container>
      <h3>Selected Users</h3>
      <Container>
        <PaginatedItemOverview onLoad={_onLoad} overrideList={users} onDeleteItem={_onUnassignUser} />
      </Container>
    </SectionComponent>
  );
};

export default UsersSection;
