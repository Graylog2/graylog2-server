// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import { type ThemeInterface } from 'theme';
import { DataTable, Spinner, PaginatedList } from 'components/common';
import { Col, Row } from 'components/graylog';

import RolesOverviewItem from './RolesOverviewItem';
import RolesFilter from './RolesFilter';

const TABLE_HEADERS = ['Name', 'Description', 'Actions'];
const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
  page: 1,
  perPage: 10,
  query: '',
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  .data-table {
    overflow-x: visible;
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => `
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _headerCellFormatter = (header) => {
  switch (header.toLocaleLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const _onPageChange = (query, loadRoles, setLoading) => (page, perPage) => {
  setLoading(true);

  return loadRoles(page, perPage, query).then(() => setLoading(false));
};

const RolesOverview = () => {
  const [loading, setLoading] = useState(false);
  const [paginatedRoles, setPaginatedRoles] = useState({ list: undefined, pagination: DEFAULT_PAGINATION });
  const { list: roles, pagination: { page, perPage, query, total } } = paginatedRoles;

  const _loadRoles = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return AuthzRolesActions.loadPaginated(newPage, newPerPage, newQuery).then(setPaginatedRoles);
  };

  const _rolesOverviewItem = (role) => <RolesOverviewItem role={role} />;
  const _handleSearch = (newQuery) => _loadRoles(1, undefined, newQuery);
  const _handleReset = () => _loadRoles(1, perPage, '');

  useEffect(() => {
    _loadRoles();

    const unlistenDeleteRole = AuthzRolesActions.deleteRole.completed.listen(() => {
      _loadRoles();
    });

    return () => {
      unlistenDeleteRole();
    };
  }, []);

  if (!roles) {
    return <Spinner />;
  }

  return (
    <Container>
      <Row className="content">
        <Col xs={12}>
          <Header>
            <h2>Roles</h2>
            {loading && <LoadingSpinner text="" delay={0} />}
          </Header>
          <p className="description">
            Found {total} roles on the system.
          </p>
          <StyledPaginatedList onChange={_onPageChange(query, _loadRoles, setLoading)} totalItems={total} activePage={page}>
            <DataTable id="roles-overview"
                       className="table-hover"
                       rowClassName="no-bm"
                       headers={TABLE_HEADERS}
                       headerCellFormatter={_headerCellFormatter}
                       sortByKey="name"
                       rows={roles.toJS()}
                       customFilter={<RolesFilter onSearch={_handleSearch} onReset={_handleReset} />}
                       dataRowFormatter={_rolesOverviewItem}
                       filterKeys={[]}
                       filterLabel="Filter Roles" />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default RolesOverview;
