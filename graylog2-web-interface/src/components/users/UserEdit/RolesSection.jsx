// @flow strict
import * as React from 'react';
import { useState } from 'react';

import UserNotification from 'util/UserNotification';
import User from 'logic/users/User';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import PaginatedItemOverview, { type PaginationInfo, type PaginatedListType } from '../PaginatedItemOverview';
import SectionComponent from '../SectionComponent';
import RolesSelector from './RolesSelector';

type Props = {
  user: User,
  onUpdate: (User) => void,
};

const RolesSection = ({ user, onUpdate }: Props) => {
  const { username } = user;
  const [loading, setLoading] = useState(false);

  const _onLoad = ({ page, perPage, query }: PaginationInfo): Promise<PaginatedListType> => {
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

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <RolesSelector onUpdate={onUpdate} user={user} />
      <PaginatedItemOverview onLoad={_onLoad} />
    </SectionComponent>
  );
};

export default RolesSection;
