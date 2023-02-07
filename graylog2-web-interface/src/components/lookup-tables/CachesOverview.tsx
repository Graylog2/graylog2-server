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

import { OverlayTrigger, PaginatedList, SearchForm, Icon, NoEntitiesExist, NoSearchResult } from 'components/common';
import { Row, Col, Table, Popover, Button } from 'components/bootstrap';
import CacheTableEntry from 'components/lookup-tables/CacheTableEntry';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { LookupTableCachesActions } from 'stores/lookup-tables/LookupTableCachesStore';
import type { LookupTableCache, PaginationType } from 'logic/lookup-tables/types';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';

import Styles from './Overview.css';

const ScrollContainer = styled.div`
  overflow-x: auto;
`;

const buildHelpPopover = () => (
  <Popover id="search-query-help" className={Styles.popoverWide} title="Search Syntax Help">
    <p><strong>Available search fields</strong></p>
    <Table condensed>
      <thead>
        <tr>
          <th>Field</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>id</td>
          <td>Cache ID</td>
        </tr>
        <tr>
          <td>title</td>
          <td>The title of the cache</td>
        </tr>
        <tr>
          <td>name</td>
          <td>The reference name of the cache</td>
        </tr>
        <tr>
          <td>description</td>
          <td>The description of cache</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find caches by parts of their names:<br />
      <kbd>name:guava</kbd><br />
      <kbd>name:gua</kbd>
    </p>
    <p>
      Searching without a field name matches against the <code>title</code> field:<br />
      <kbd>guava</kbd> <br />is the same as<br />
      <kbd>title:guava</kbd>
    </p>
  </Popover>
);

type Props = {
  caches: LookupTableCache[],
  pagination: PaginationType,
  paginationQueryParameter: PaginationQueryParameterResult,
};

const queryHelpComponent = (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={buildHelpPopover()}>
    <Button bsStyle="link"
            className={Styles.searchHelpButton}>
      <Icon name="question-circle" fixedWidth />
    </Button>
  </OverlayTrigger>
);

const CachesOverview = ({ caches, pagination, paginationQueryParameter }: Props) => {
  const { currentPage, currentPageSize, resetPage } = React.useMemo(() => ({
    currentPage: paginationQueryParameter.page || 1,
    currentPageSize: paginationQueryParameter.pageSize || 10,
    resetPage: paginationQueryParameter.resetPage,
  }), [paginationQueryParameter]);

  const onPageChange = React.useCallback((newPage: number, newPerPage: number) => {
    LookupTableCachesActions.searchPaginated(newPage, newPerPage, pagination.query);
  }, [pagination.query]);

  const onSearch = React.useCallback((query: string, resetLoadingStateCb: () => void) => {
    resetPage();
    LookupTableCachesActions.searchPaginated(currentPage, currentPageSize, query).then(resetLoadingStateCb);
  }, [resetPage, currentPage, currentPageSize]);

  const onReset = React.useCallback(() => {
    resetPage();
    LookupTableCachesActions.searchPaginated(currentPage, currentPageSize);
  }, [resetPage, currentPage, currentPageSize]);

  const emptyListComponent = pagination.query === ''
    ? (<NoEntitiesExist>No Cache exist.</NoEntitiesExist>)
    : (<NoSearchResult>No Cache found.</NoSearchResult>);

  return (
    <Row className="content">
      <Col md={12}>
        <h2 style={{ marginBottom: 16 }}>
          Configured lookup Caches <small>{pagination.total} total</small>
        </h2>
        <PaginatedList activePage={currentPage}
                       pageSize={currentPageSize}
                       onChange={onPageChange}
                       totalItems={pagination.total}>
          <SearchForm onSearch={onSearch} onReset={onReset} queryHelpComponent={queryHelpComponent} />
          <ScrollContainer>
            {caches.length === 0
              ? (emptyListComponent)
              : (
                <Table condensed hover className={Styles.overviewTable}>
                  <thead>
                    <tr>
                      <th className={Styles.rowTitle}>Title</th>
                      <th className={Styles.rowDescription}>Description</th>
                      <th className={Styles.rowName}>Name</th>
                      <th>Entries</th>
                      <th>Hit rate</th>
                      <th>Throughput</th>
                      <th className={Styles.rowActions}>Actions</th>
                    </tr>
                  </thead>
                  {caches.map((cache: LookupTableCache) => (
                    <CacheTableEntry key={cache.id} cache={cache} />
                  ))}
                </Table>
              )}
          </ScrollContainer>
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default withPaginationQueryParameter(CachesOverview);
