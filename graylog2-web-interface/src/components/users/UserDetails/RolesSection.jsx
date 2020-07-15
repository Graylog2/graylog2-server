// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import User from 'logic/users/User';
import AuthzRolesActions from 'actions/roles/AuthzRolesActions';
import type { PaginatedList } from 'stores/roles/AuthzRolesStore';

import SectionComponent from '../SectionComponent';

type Props = {
  user: User,
};

const RolesSection = ({ user: { username } }: Props) => {
  const [roles, setRoles] = useState();
  const [paginationInfo, setPaginationInfo] = useState({
    count: undefined,
    total: undefined,
    page: 1,
    perPage: 5,
    query: '',
  });

  useEffect(() => {
    const { page, perPage, query } = paginationInfo;
    console.log("useEffekt", username);

    AuthzRolesActions.loadForUser(username, page, perPage, query).then(({ list, pagination }: PaginatedList) => {
      setPaginationInfo(pagination);
      setRoles(list);
    });
  }, []);

  return (
    <SectionComponent title="Roles">
      <>
        {roles && roles.map((role) => <div>{role.name}</div>)}
      </>
    </SectionComponent>
  );
};

export default RolesSection;
