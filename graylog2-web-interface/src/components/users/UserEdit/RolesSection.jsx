// @flow strict
import * as React from 'react';
import { useState } from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import UserNotification from 'util/UserNotification';
import User from 'logic/users/User';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import { UsersActions } from 'stores/users/UsersStore';
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

  const _onLoad = ({ page, perPage, query }: PaginationInfo = defaultPageInfo): Promise<PaginatedListType> => {
    setLoading(true);

    return AuthzRolesActions.loadForUser(username, page, perPage, query)
      .then((response) => {
        setLoading(false);

        return {
          pagination: response.pagination,
          list: response.list.map((item) => (item: DescriptiveItem)),
        };
      }, (error) => {
        if (error.additional.status === 404) {
          UserNotification.error(`Loading roles for user ${username} failed with status: ${error}`,
            'Could not load roles for user');
        }

        return error;
      });
  };

  const onRolesUpdate = (data: {roles: Array<string>}) => onSubmit(data).then(() => {
    _onLoad().then((response) => setRoles(response));
    UsersActions.load(username);
  });

  const _onAssignRole = (newRole: DescriptiveItem) => {
    const userRoles = user.roles;
    const newRoles = userRoles.push(newRole.name).toJS();

    return onRolesUpdate({ roles: newRoles });
  };

  const onDeleteRole = (role: DescriptiveItem) => {
    const newUserRoles = Immutable.Set(user.roles.toJS()).remove(role.name).toJS();

    onRolesUpdate({ roles: newUserRoles });
  };

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <h3>Assign Roles</h3>
      <Container>
        <RolesSelector onSubmit={_onAssignRole} user={user} />
      </Container>
      <h3>Selected Roles</h3>
      <Container>
        <PaginatedItemOverview onLoad={_onLoad}
                               overrideList={roles}
                               onDeleteItem={onDeleteRole}
                               queryHelper={<RolesQueryHelp />} />
      </Container>
    </SectionComponent>
  );
};

export default RolesSection;
