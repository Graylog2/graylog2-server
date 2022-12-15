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

import { OverlayTrigger, PaginatedList, SearchForm, Spinner, Icon } from 'components/common';
import { Row, Col, Table, Popover, Button } from 'components/bootstrap';
import DataAdapterTableEntry from 'components/lookup-tables/DataAdapterTableEntry';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import type { LookupTableAdapter, PaginationType } from 'logic/lookup-tables/types';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';

import Styles from './Overview.css';

const buildHelpPopover = () => {
  return (
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
            <td>Data Adapter ID</td>
          </tr>
          <tr>
            <td>title</td>
            <td>The title of the data adapter</td>
          </tr>
          <tr>
            <td>name</td>
            <td>The reference name of the data adapter</td>
          </tr>
          <tr>
            <td>description</td>
            <td>The description of data adapter</td>
          </tr>
        </tbody>
      </Table>
      <p><strong>Example</strong></p>
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
    </Popover>
  );
};

type Props = {
  dataAdapters: LookupTableAdapter[],
  pagination: PaginationType,
  errorStates: { [key: string]: { [key: string]: string } },
  paginationQueryParameter: PaginationQueryParameterResult,
};

const DataAdaptersOverview = ({ dataAdapters, pagination, errorStates, paginationQueryParameter }: Props) => {
  const { currentPage, currentPageSize, resetPage } = React.useMemo(() => ({
    currentPage: paginationQueryParameter.page || 1,
    currentPageSize: paginationQueryParameter.pageSize || 10,
    resetPage: paginationQueryParameter.resetPage,
  }), [paginationQueryParameter]);

  const onPageChange = (newPage: number, newPerPage: number) => {
    LookupTableDataAdaptersActions.searchPaginated(newPage, newPerPage, pagination.query);
  };

  const onSearch = React.useCallback((query: string, resetLoadingStateCb: () => void) => {
    resetPage();
    LookupTableDataAdaptersActions.searchPaginated(currentPage, currentPageSize, query).then(resetLoadingStateCb);
  }, [resetPage, currentPage, currentPageSize]);

  const onReset = React.useCallback(() => {
    resetPage();
    LookupTableDataAdaptersActions.searchPaginated(currentPage, currentPageSize);
  }, [resetPage, currentPage, currentPageSize]);

  return (
    <Row className="content">
      <Col md={12}>
        <h2 style={{ marginBottom: 16 }}>
          Configured lookup Data Adapters <small>{pagination.total} total</small>
        </h2>
        <PaginatedList activePage={currentPage}
                       pageSize={currentPageSize}
                       onChange={onPageChange}
                       totalItems={pagination.total}>
          <SearchForm onSearch={onSearch} onReset={onReset}>
            <OverlayTrigger trigger="click" rootClose placement="right" overlay={buildHelpPopover()}>
              <Button bsStyle="link"
                      className={Styles.searchHelpButton}>
                <Icon name="question-circle" fixedWidth />
              </Button>
            </OverlayTrigger>
          </SearchForm>
          <div style={{ overflowX: 'auto' }}>
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
              {dataAdapters.length === 0
                ? <Spinner text="Loading data adapters" />
                : dataAdapters.map((dataAdapter: LookupTableAdapter) => (
                  <DataAdapterTableEntry key={dataAdapter.id}
                                         adapter={dataAdapter}
                                         error={errorStates.dataAdapters[dataAdapter.name]} />
                ))}
            </Table>
          </div>
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default withPaginationQueryParameter(DataAdaptersOverview);
