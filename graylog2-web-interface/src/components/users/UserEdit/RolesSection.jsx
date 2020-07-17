// @flow strict
import * as React from 'react';
import { useState } from 'react';

import UserNotification from 'util/UserNotification';
import User from 'logic/users/User';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import RolesSelector from './RolesSelector';

import PaginatedItemOverview, { defaultPageInfo, type PaginationInfo, type PaginatedListType } from '../PaginatedItemOverview';
import SectionComponent from '../SectionComponent';

type Props = {
  user: User,
  onSubmit: ({ roles: string[] }) => Promise<void>,
};

const RolesSection = ({ user, onSubmit }: Props) => {
  const { username } = user;
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState();

  const _onLoad = ({ page, perPage, query }: PaginationInfo = defaultPageInfo): Promise<PaginatedListType> => {
    setLoading(true);

    // $FlowFixMe Role has DescriptiveItem implemented!!!
    return AuthzRolesActions.loadForUser(username, page, perPage, query)
      .then((response) => {
        setLoading(false);

        return response;
      }).catch((error) => {
        if (error.additional.status === 404) {
          UserNotification.error(`Loading roles for user ${username} failed with status: ${error}`,
            'Could not load roles for user');
        }
      });
  };

  const onUpdate = (data) => onSubmit(data).then(() => _onLoad().then((response) => setRoles(response)));

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <RolesSelector onSubmit={onUpdate} user={user} />
      <PaginatedItemOverview onLoad={_onLoad} overrideList={roles} onDeleteItem={(x) => console.log(x)}/>
    </SectionComponent>
  );
};

export default RolesSection;
