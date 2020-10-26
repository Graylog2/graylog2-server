// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';

import SharedEntitiesOverview from 'components/permissions/SharedEntitiesOverview';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  userId: $PropertyType<User, 'id'>,
};

const SharedEntitiesSection = ({ userId }: Props) => {
  const [loading, setLoading] = useState(false);
  const _searchPaginated = useCallback((pagination) => EntityShareDomain.loadUserSharesPaginated(userId, pagination), [userId]);

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      <SharedEntitiesOverview setLoading={setLoading}
                              entityType="user"
                              searchPaginated={_searchPaginated} />
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
