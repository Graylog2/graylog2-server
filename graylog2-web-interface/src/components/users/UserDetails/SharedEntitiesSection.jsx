// @flow strict
import * as React from 'react';
import { useState } from 'react';

import User from 'logic/users/User';
import { type PaginatedUserSharesType } from 'stores/permissions/EntityShareStore';
import { Spinner } from 'components/common';

import SharedEntitiesOverview from './SharedEntitiesOverview';

import SectionComponent from '../SectionComponent';

type Props = {
  username: $PropertyType<User, 'username'>,
  paginatedUserShares: ?PaginatedUserSharesType,
};

const SharedEntitiesSection = ({ paginatedUserShares, username }: Props) => {
  const [loading, setLoading] = useState(false);

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      {paginatedUserShares ? (
        <SharedEntitiesOverview paginatedUserShares={paginatedUserShares} username={username} setLoading={setLoading} />
      ) : (
        <Spinner />
      )}
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
