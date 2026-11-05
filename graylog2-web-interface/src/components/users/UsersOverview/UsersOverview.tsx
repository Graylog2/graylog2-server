/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import styled, { css } from 'styled-components';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';

import UsersDomain from 'domainActions/users/UsersDomain';
import { USERS_QUERY_KEY } from 'hooks/useUsers';
import useCurrentUser from 'hooks/useCurrentUser';
import { DataTable, Spinner, PaginatedList, NoSearchResult } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import type UserOverview from 'logic/users/UserOverview';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import UserOverviewItem from './UserOverviewItem';
import UsersFilter from './UsersFilter';
import ClientAddressHead from './ClientAddressHead';
import SystemAdministrator from './SystemAdministratorOverview';

const TABLE_HEADERS = ['', 'full_name', 'username', 'email', 'client_address', 'enabled', 'role', 'actions'];

const Container = styled.div`
  .data-table {
    overflow-x: visible;
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(
  ({ theme }) => css`
    margin-left: 10px;
    font-size: ${theme.fonts.size.h3};
  `,
);

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _headerCellFormatter = (t: TFunction) => (header: string) => {
  const title = header ? t(`users_list.headers.${header}`) : header;

  switch (header) {
    case 'client_address':
      return <ClientAddressHead title={title} />;
    case 'actions':
      return <th className="actions text-right">{title}</th>;
    default:
      return <th>{title}</th>;
  }
};

const buildUsersOverviewItem =
  (currentUser: any, onDeleted: () => void, onStatusChange: () => void) => (user: UserOverview) => {
    const { id: userId } = user;

    return (
      <UserOverviewItem
        user={user}
        isActive={currentUser?.id === userId}
        onDeleted={onDeleted}
        onStatusChange={onStatusChange}
      />
    );
  };

const UsersOverview = () => {
  const { t } = useTranslation('users');
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');

  const { data: paginatedUsers, isFetching } = useQuery({
    queryKey: [...USERS_QUERY_KEY, 'overview-paginated', { page, perPage, query }],
    queryFn: () => UsersDomain.loadUsersPaginated({ page, perPage, query }),
    placeholderData: keepPreviousData,
    retry: false,
  });

  const { list: users, adminUser, pagination: { total = 0 } = {} } = paginatedUsers || {};

  const onUserDeleted = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY });
    resetPage();
  }, [queryClient, resetPage]);

  const onUserStatusChange = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY });
  }, [queryClient]);

  if (!users) {
    return <Spinner />;
  }

  const handleSearch = (newQuery: string) => {
    resetPage();
    setQuery(newQuery);
  };

  const searchFilter = <UsersFilter onSearch={handleSearch} />;

  return (
    <Container>
      {adminUser && (
        <SystemAdministrator
          adminUser={adminUser}
          dataRowFormatter={buildUsersOverviewItem(currentUser, onUserDeleted, onUserStatusChange)}
          headerCellFormatter={_headerCellFormatter(t)}
          headers={TABLE_HEADERS}
        />
      )}
      <Row className="content">
        <Col xs={12}>
          <Header>
            <h2>{t('users_list.title')}</h2>
            {isFetching && <LoadingSpinner text="" delay={0} />}
          </Header>
          <p className="description">{t('users_list.found', { total })}</p>
          <StyledPaginatedList totalItems={total}>
            <DataTable
              id="users-overview"
              rowClassName="no-bm"
              headers={TABLE_HEADERS}
              headerCellFormatter={_headerCellFormatter(t)}
              sortByKey="fullName"
              noDataText={<NoSearchResult>{t('users_list.none_found')}</NoSearchResult>}
              rows={users.toJS()}
              customFilter={searchFilter}
              dataRowFormatter={buildUsersOverviewItem(currentUser, onUserDeleted, onUserStatusChange)}
              filterKeys={[]}
              filterLabel={t('users_list.filter_label')}
            />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default UsersOverview;
