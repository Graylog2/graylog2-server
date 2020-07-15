// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import { PaginatedList, SearchForm } from 'components/common';
import User from 'logic/users/User';
import { AuthzRolesActions, type PaginatedListType } from 'stores/roles/AuthzRolesStore';

import RoleItem from './RoleItem';

import SectionComponent from '../SectionComponent';

const Container = styled.div`
  margin-top: 10px;
`;

type Props = {
  user: User,
};

const RolesSection = ({ user: { username } }: Props) => {
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState();
  const [paginationInfo, setPaginationInfo] = useState({
    count: undefined,
    total: 0,
    page: 1,
    perPage: 5,
    query: '',
  });

  const _setResponse = ({ list, pagination }: PaginatedListType) => {
    setPaginationInfo(pagination);
    setRoles(list);
    setLoading(false);
  };

  useEffect(() => {
    const { page, perPage, query } = paginationInfo;
    setLoading(true);

    AuthzRolesActions.loadForUser(username, page, perPage, query).then(_setResponse);
  }, []);

  const _onPageChange = (query) => (page, perPage) => {
    setLoading(true);
    AuthzRolesActions.loadForUser(username, page, perPage, query).then(_setResponse);
  };

  const _onSearch = (query) => {
    setLoading(true);
    AuthzRolesActions.loadForUser(username, 1, paginationInfo.perPage, query).then(_setResponse);
  };

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <PaginatedList onChange={_onPageChange(paginationInfo.query)}
                     pageSize={paginationInfo.perPage}
                     totalItems={paginationInfo.total}
                     pageSizes={[5, 10, 30]}
                     activePage={paginationInfo.page}>
        <SearchForm onSearch={_onSearch} />
        <Container>
          {roles && roles.toArray().map((role) => <RoleItem key={role.id} role={role} />) }
        </Container>
      </PaginatedList>
    </SectionComponent>
  );
};

export default RolesSection;
