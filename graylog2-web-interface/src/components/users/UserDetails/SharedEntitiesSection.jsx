// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';

import SharedEntitiesOverview from 'components/permissions/SharedEntitiesOverview';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  username: $PropertyType<User, 'username'>,
};

const SharedEntitiesSection = ({ username }: Props) => {
  const [loading, setLoading] = useState(false);
  const _searchPaginated = useCallback((pagination) => EntityShareDomain.loadUserSharesPaginated(username, pagination), [username]);

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      <SharedEntitiesOverview setLoading={setLoading}
                              entityType="user"
                              searchPaginated={_searchPaginated} />
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
