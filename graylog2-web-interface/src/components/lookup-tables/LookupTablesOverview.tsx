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

import { Row, Col, Table, Popover, Button } from 'components/bootstrap';
import { OverlayTrigger, PaginatedList, SearchForm, Icon } from 'components/common';
import LUTTableEntry from 'components/lookup-tables/LUTTableEntry';
import type { LookupTable, LookupTableAdapter, LookupTableCache } from 'logic/lookup-tables/types';
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';

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
            <td>Lookup Table ID</td>
          </tr>
          <tr>
            <td>title</td>
            <td>The title of the lookup table</td>
          </tr>
          <tr>
            <td>name</td>
            <td>The reference name of the lookup table</td>
          </tr>
          <tr>
            <td>description</td>
            <td>The description of lookup table</td>
          </tr>
        </tbody>
      </Table>
      <p><strong>Examples</strong></p>
      <p>
        Find lookup tables by parts of their names:<br />
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

const getLookupTablesList = (
  table: LookupTable,
  caches: LookupTableCache[],
  dataAdapters: LookupTableAdapter[],
  errorStates: { [key: string]: { [key: string]: string } },
) => {
  const lookupName = (id: string, map: LookupTableCache[] | LookupTableAdapter[]) => {
    const empty = { title: 'none' };

    if (!map) return empty;

    return map[id] || empty;
  };

  const lookupAdapterError = () => {
    if (errorStates.dataAdapters && dataAdapters) {
      const adapter = dataAdapters[table.data_adapter_id];

      if (!adapter) return null;

      return errorStates.dataAdapters[adapter.name];
    }

    return null;
  };

  const cache = lookupName(table.cache_id, caches);
  const dataAdapter = lookupName(table.data_adapter_id, dataAdapters);
  const errors = {
    table: errorStates.tables[table.name],
    cache: null,
    dataAdapter: lookupAdapterError(),
  };

  return (
    <LUTTableEntry key={table.id}
                   table={table}
                   cache={cache}
                   dataAdapter={dataAdapter}
                   errors={errors} />
  );
};

type OverviewProps = {
  tables: LookupTable[],
  caches: LookupTableCache[],
  dataAdapters: LookupTableAdapter[],
  pagination: { page: number, per_page: number, total: number, query?: string },
  errorStates: { [key: string]: { [key: string]: string } },
};

const LookupTablesOverview = ({ tables, caches, dataAdapters, pagination, errorStates }: OverviewProps) => {
  const onPageChange = (newPage: number, newPerPage: number) => {
    LookupTablesActions.searchPaginated(newPage, newPerPage, pagination.query);
  };

  const onSearch = (query: string) => {
    LookupTablesActions.searchPaginated(1, pagination.per_page, query);
  };

  const onReset = () => {
    LookupTablesActions.searchPaginated(1, pagination.per_page, null);
  };

  return (
    <Row className="content">
      <Col md={12}>
        <h2>
          Configured lookup tables
          <span>&nbsp;<small>{pagination.total} total</small></span>
        </h2>
        <PaginatedList activePage={pagination.page}
                       pageSize={pagination.per_page}
                       onChange={onPageChange}
                       totalItems={pagination.total}>
          <SearchForm onSearch={onSearch} onReset={onReset}>
            <OverlayTrigger trigger="click" rootClose placement="right" overlay={getHelpPopover()}>
              <Button bsStyle="link" className={Styles.searchHelpButton}><Icon name="question-circle" fixedWidth /></Button>
            </OverlayTrigger>
          </SearchForm>
          <Table condensed hover className={Styles.overviewTable}>
            <thead>
              <tr>
                <th className={Styles.rowTitle}>Title</th>
                <th className={Styles.rowDescription}>Description</th>
                <th className={Styles.rowName}>Name</th>
                <th className={Styles.rowCache}>Cache</th>
                <th className={Styles.rowAdapter}>Data Adapter</th>
                <th className={Styles.rowActions}>Actions</th>
              </tr>
            </thead>
            {tables.map((table: LookupTable) => getLookupTablesList(table, caches, dataAdapters, errorStates))}
          </Table>
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default LookupTablesOverview;
