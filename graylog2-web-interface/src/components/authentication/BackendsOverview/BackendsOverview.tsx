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
import { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import type { PaginatedRoles } from 'hooks/useAuthzRoles';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { AUTHENTICATION_QUERY_KEY } from 'hooks/useAuthentication';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import type { AuthenticationBackendJSON } from 'logic/authentication/AuthenticationBackend';

import BackendsFilter from './BackendsFilter';
import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Description', 'Default Roles', 'Actions'];
const HEADER_TITLE_KEYS = {
  Title: 'backends_overview.headers.title',
  Description: 'backends_overview.headers.description',
  'Default Roles': 'backends_overview.headers.default_roles',
  Actions: 'backends_overview.headers.actions',
};

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

const _loadRoles = () => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  return AuthzRolesDomain.loadRolesPaginated(getUnlimited);
};

const _backendsOverviewItem = (
  authBackend: AuthenticationBackend,
  context: { activeBackend: AuthenticationBackendJSON },
  paginatedRoles: PaginatedRoles,
) => (
  <BackendsOverviewItem
    authenticationBackend={authBackend}
    isActive={authBackend.id === context?.activeBackend?.id}
    roles={paginatedRoles.list}
  />
);

const BackendsOverview = () => {
  const { t } = useTranslation('authentication');
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined>();
  const [query, setQuery] = useState('');

  useEffect(() => {
    _loadRoles().then(setPaginatedRoles);
  }, []);

  const { data: paginatedBackends, isFetching } = useQuery({
    queryKey: [...AUTHENTICATION_QUERY_KEY, 'backends', { query, page, perPage }],
    queryFn: () => AuthenticationDomain.loadBackendsPaginated({ query, page, perPage }),
  });

  const { list: backends, context } = paginatedBackends || {};

  const onSearch = (newQuery: string, resetLoadingStateCb: () => void) => {
    resetPage();
    setQuery(newQuery);

    if (!isFetching && resetLoadingStateCb) {
      resetLoadingStateCb();
    }
  };

  const _headerCellFormatter = (header: string) => {
    const title = t(HEADER_TITLE_KEYS[header]);

    switch (header.toLowerCase()) {
      case 'actions':
        return <th className="actions text-right">{title}</th>;
      default:
        return <th>{title}</th>;
    }
  };

  if (!paginatedBackends || !paginatedRoles) {
    return <Spinner />;
  }

  return (
    <Row className="content">
      <Col xs={12}>
        <h2>{t('backends_overview.title')}</h2>
        <Header>{isFetching && <LoadingSpinner text="" delay={0} />}</Header>
        <p className="description">{t('backends_overview.found', { count: paginatedBackends.pagination.total })}</p>
        <PaginatedList totalItems={paginatedBackends.pagination.total}>
          <DataTable
            customFilter={<BackendsFilter onSearch={onSearch} />}
            dataRowFormatter={(authBackend) => _backendsOverviewItem(authBackend, context, paginatedRoles)}
            filterKeys={[]}
            filterLabel={t('backends_overview.filter_label')}
            headerCellFormatter={_headerCellFormatter}
            headers={TABLE_HEADERS}
            id="auth-backends-overview"
            rowClassName="no-bm"
            rows={backends.toJS()}
            sortByKey="title"
          />
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default BackendsOverview;
