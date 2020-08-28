// @flow strict
import * as React from 'react';
import { useState } from 'react';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import UserNotification from 'util/UserNotification';
import User from 'logic/users/User';
import PaginatedItemOverview, {
  type PaginationInfo,
  type PaginatedListType,
} from 'components/common/PaginatedItemOverview';
import SectionComponent from 'components/common/Section/SectionComponent';

import RolesQueryHelp from '../RolesQueryHelp';

type Props = {
  user: User,
};

const RolesSection = ({ user: { username } }: Props) => {
  const [loading, setLoading] = useState(false);

  const _onLoad = ({ page, perPage, query }: PaginationInfo, isSubscribed: boolean): Promise<?PaginatedListType> => {
    setLoading(true);

    return AuthzRolesDomain.loadRolesForUser(username, page, perPage, query)
      .then((response) => {
        if (isSubscribed) {
          setLoading(false);
        }

        // $FlowFixMe Role has DescriptiveItem implemented!!!
        return response;
      }).catch((error) => {
        if (error?.additional?.status === 404) {
          UserNotification.error(`Loading roles for user ${username} failed with status: ${error}`,
            'Could not load roles for user');
        }
      });
  };

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <PaginatedItemOverview onLoad={_onLoad} queryHelper={<RolesQueryHelp />} />
    </SectionComponent>
  );
};

export default RolesSection;
