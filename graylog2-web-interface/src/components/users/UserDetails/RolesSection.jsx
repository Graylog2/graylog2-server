// @flow strict
import * as React from 'react';
import { useState } from 'react';

import User from 'logic/users/User';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import PaginatedItemOverview, { type PaginationInfo, type PaginatedListType } from '../PaginatedItemOverview';
import SectionComponent from '../SectionComponent';

type Props = {
  user: User,
};

const RolesSection = ({ user: { username } }: Props) => {
  const [loading, setLoading] = useState(false);

  const _onLoad = ({ page, perPage, query }: PaginationInfo): Promise<PaginatedListType> => {
    setLoading(true);

    // $FlowFixMe Role implements Descriptive Item!!!
    return AuthzRolesActions.loadForUser(username, page, perPage, query)
      .then(setLoading(false));
  };

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <PaginatedItemOverview onLoad={_onLoad} />
    </SectionComponent>
  );
};

export default RolesSection;
