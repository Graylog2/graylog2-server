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
import { useHistory } from 'react-router-dom';
import { useQueryClient } from 'react-query';

import Routes from 'routing/Routes';
import { Row, Col, Table, Popover, Button } from 'components/bootstrap';
import { OverlayTrigger, PaginatedList, SearchForm, Spinner, Icon } from 'components/common';
import { useGetAllDataAdapters, useGetDataAdapterErrors } from 'hooks/lookup-tables/useLookupTableDataAdaptersAPI';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

import DataAdapterTableEntry from './DataAdapterTableEntry';
import Styles from './Overview.css';

const getHelpPopover = () => {
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

const DataAdaptersOverview = () => {
  const history = useHistory();
  const queryClient = useQueryClient();

  const [localPagination, setLocalPagination] = React.useState({ page: 1, perPage: 10, query: null });
  const { dataAdapters, pagination, loadingDataAdapters } = useGetAllDataAdapters(localPagination);
  const { dataAdapterErrors, loadingDataAdaptersErrors } = useGetDataAdapterErrors(dataAdapters);

  const onPageChange = (newPage: number, newPerPage: number) => {
    setLocalPagination({ ...localPagination, page: newPage, perPage: newPerPage });
  };

  const onSearch = (newQuery: string) => {
    setLocalPagination({ ...localPagination, page: 1, query: newQuery });
  };

  const onReset = () => {
    setLocalPagination({ ...localPagination, query: null });
  };

  const onDelete = () => {
    queryClient.invalidateQueries('all-data-adapters');
  };

  const toCreateView = () => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE);
  };

  return (loadingDataAdapters || loadingDataAdaptersErrors) ? <Spinner text="Loading data adapters" /> : (
    <Row className="content">
      <Col md={12}>
        <h2>
          Configured lookup Data Adapters
          <span>&nbsp;
            <small>{pagination.total} total</small>
          </span>
        </h2>
        <PaginatedList activePage={localPagination.page}
                       pageSize={localPagination.perPage}
                       onChange={onPageChange}
                       totalItems={pagination.total}>
          <SearchForm onSearch={onSearch} onReset={onReset}>
            <Button bsStyle="success"
                    onClick={toCreateView}
                    style={{ marginLeft: 5 }}>
              Create data adapter
            </Button>
            <OverlayTrigger trigger="click" rootClose placement="right" overlay={getHelpPopover()}>
              <Button bsStyle="link"
                      className={Styles.searchHelpButton}>
                <Icon name="question-circle" fixedWidth />
              </Button>
            </OverlayTrigger>
          </SearchForm>
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
            {dataAdapters.map((adapter: LookupTableAdapter) => (
              <DataAdapterTableEntry key={adapter.id}
                                     adapter={adapter}
                                     error={dataAdapterErrors[adapter.name]}
                                     onDelete={onDelete} />
            ))}
          </Table>
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default DataAdaptersOverview;
