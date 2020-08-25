// @flow strict
import * as React from 'react';
import { useState } from 'react';

import SharedEntitiesOverview from 'components/permissions/SharedEntitiesOverview';
import type { PaginatedEnititySharesType } from 'actions/permissions/EntityShareActions';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import User from 'logic/users/User';
import { Spinner } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  username: $PropertyType<User, 'username'>,
  paginatedUserShares: ?PaginatedEnititySharesType,
};

const SharedEntitiesSection = ({ paginatedUserShares, username }: Props) => {
  const [loading, setLoading] = useState(false);
  const _searchPaginated = (newPage, newPerPage, newQuery, additonalQueries) => EntityShareDomain.loadUserSharesPaginated(username, newPage, newPerPage, newQuery, additonalQueries);

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      {paginatedUserShares ? (
        <SharedEntitiesOverview paginatedEntityShares={paginatedUserShares}
                                setLoading={setLoading}
                                entityType="user"
                                searchPaginated={_searchPaginated} />
      ) : (
        <Spinner />
      )}
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
