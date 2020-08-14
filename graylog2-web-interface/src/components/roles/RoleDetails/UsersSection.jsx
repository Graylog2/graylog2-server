// @flow strict
import * as React from 'react';
import { useState } from 'react';

import UserNotification from 'util/UserNotification';
import { PaginatedItemOverview } from 'components/common';
import type { PaginationInfo, PaginatedListType } from 'components/common/PaginatedItemOverview';
import SectionComponent from 'components/common/Section/SectionComponent';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import Role from 'logic/roles/Role';

type Props = {
  role: Role,
};

const UsersSection = ({ role: { id } }: Props) => {
  const [loading, setLoading] = useState(false);

  const _onLoad = ({ page, perPage, query }: PaginationInfo): Promise<?PaginatedListType> => {
    setLoading(true);

    return AuthzRolesActions.loadUsersForRole(id, page, perPage, query)
      .then((response) => {
        setLoading(false);

        // $FlowFixMe UserOverview is a DescriptiveItem!!!
        return response;
      }).catch((error) => {
        if (error.additional.status === 404) {
          UserNotification.error(`Loading users for role with id ${id} failed with status: ${error}`,
            'Could not load users for role');
        }
      });
  };

  return (
    <SectionComponent title="Users" showLoading={loading}>
      <PaginatedItemOverview onLoad={_onLoad} />
    </SectionComponent>
  );
};

export default UsersSection;
