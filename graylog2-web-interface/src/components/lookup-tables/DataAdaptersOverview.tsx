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
import React from 'react';
import styled from 'styled-components';

import { Row, Col, Table } from 'components/bootstrap';
import {
  PaginatedList,
  SearchForm,
  Spinner,
  NoSearchResult,
  NoEntitiesExist,
} from 'components/common';
import DataAdapterTableEntry from 'components/lookup-tables/DataAdapterTableEntry';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import type { LookupTableAdapter, PaginationType } from 'logic/lookup-tables/types';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import QueryHelper from 'components/common/QueryHelper';

import Styles from './Overview.css';

const ScrollContainer = styled.div`
  overflow-x: auto;
`;

const queryExamples = (
  <>
    <p>
      Find data adapters by parts of their names:<br />
      <kbd>name:geoip</kbd><br />
      <kbd>name:geo</kbd>
    </p>
    <p>
      Searching without a field name matches against the <code>title</code> field:<br />
      <kbd>geoip</kbd> <br />is the same as<br />
      <kbd>title:geoip</kbd>
    </p>
  </>
);

const queryHelpComponent = (
  <QueryHelper entityName="data adapter"
               commonFields={['id', 'title', 'name', 'description']}
               example={queryExamples} />
);

const NoResults = ({ query }: { query: string }) => (
  <tbody>
    <tr>
      <td colSpan={5}>
        {query
          ? <NoSearchResult>No data adapters found with title &quot;{query}&quot;</NoSearchResult>
          : <NoEntitiesExist>There are no data adapters to list</NoEntitiesExist>}
      </td>
    </tr>
  </tbody>
);

const DataRow = ({
  dataAdapters,
  query,
  errors,
}: {
  dataAdapters: LookupTableAdapter[],
  query: string,
  errors: { [key: string]: string },
}) => (dataAdapters.length > 0
  ? (
    <>
      {dataAdapters.map((dataAdapter: LookupTableAdapter) => (
        <DataAdapterTableEntry key={dataAdapter.id}
                               adapter={dataAdapter}
                               error={errors[dataAdapter.name]} />
      ))}
    </>
  ) : (
    <NoResults query={query} />
  ));

type Props = {
  dataAdapters: LookupTableAdapter[],
  pagination: PaginationType,
  errorStates: { [key: string]: { [key: string]: string } },
  paginationQueryParameter: PaginationQueryParameterResult,
};

const DataAdaptersOverview = ({ dataAdapters, pagination, errorStates, paginationQueryParameter }: Props) => {
  const [loading, setLoading] = React.useState(false);
  const [localPagination, setLocalPagination] = React.useState({
    currentPage: paginationQueryParameter.page || 1,
    currentPageSize: paginationQueryParameter.pageSize || 10,
    currentQuery: pagination.query ? decodeURI(pagination.query) : '',
    resetPage: paginationQueryParameter.resetPage,
    setPagination: paginationQueryParameter.setPagination,
  });

  React.useEffect(() => {
    const { currentPage, currentPageSize, currentQuery } = localPagination;

    LookupTableDataAdaptersActions.searchPaginated(currentPage, currentPageSize, currentQuery)
      .then(() => setLoading(false));
  }, [localPagination]);

  const onPageChange = React.useCallback((newPage: number, newPerPage: number) => {
    setLocalPagination({ ...localPagination, currentPage: newPage, currentPageSize: newPerPage });
  }, [localPagination]);

  const onSearch = React.useCallback((query: string) => {
    localPagination.resetPage();
    localPagination.setPagination({ page: 1, pageSize: localPagination.currentPageSize });
    setLocalPagination({ ...localPagination, currentPage: 1, currentQuery: query });
  }, [localPagination]);

  const onReset = React.useCallback(() => {
    localPagination.resetPage();
    localPagination.setPagination({ page: 1, pageSize: localPagination.currentPageSize });
    setLocalPagination({ ...localPagination, currentPage: 1, currentQuery: '' });
  }, [localPagination]);

  return (
    <Row className="content">
      <Col md={12}>
        <h2 style={{ marginBottom: 16 }}>
          Configured lookup Data Adapters <small>{pagination.total} total</small>
        </h2>
        <PaginatedList activePage={localPagination.currentPage}
                       pageSize={localPagination.currentPageSize}
                       onChange={onPageChange}
                       totalItems={pagination.total}>
          <SearchForm onSearch={onSearch} onReset={onReset} queryHelpComponent={queryHelpComponent} />
          <ScrollContainer>
            <Table condensed hover className={Styles.overviewTable}>
              <thead>
                <tr>
                  <th className={Styles.rowTitle}>Title</th>
                  <th className={Styles.rowDescription}>Description</th>
                  <th className={Styles.rowName}>Name</th>
                  <th>Throughput</th>
                  <th className={Styles.rowActions}>Actions</th>
                </tr>
              </thead>
              {loading ? <Spinner text="Loading data adapters" /> : (
                <DataRow dataAdapters={dataAdapters}
                         query={localPagination.currentQuery}
                         errors={errorStates.dataAdapters} />
              )}
            </Table>
          </ScrollContainer>
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default withPaginationQueryParameter(DataAdaptersOverview);
