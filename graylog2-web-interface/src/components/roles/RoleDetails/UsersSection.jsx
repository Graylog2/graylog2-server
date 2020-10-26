// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { PaginatedItemOverview } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import Role from 'logic/roles/Role';

type Props = {
  role: Role,
};

const UsersSection = ({ role: { id, name } }: Props) => {
  const [loading, setLoading] = useState(false);

  const _onLoad = useCallback((pagination) => {
    setLoading(true);

    return AuthzRolesDomain.loadUsersForRole(id, name, pagination).then((paginatedRoles) => {
      setLoading(false);

      return paginatedRoles;
    });
  }, [id, name]);

  return (
    <SectionComponent title="Users" showLoading={loading}>
      <PaginatedItemOverview noDataText="No selected users have been found." onLoad={_onLoad} />
    </SectionComponent>
  );
};

export default UsersSection;
