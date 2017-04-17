import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Table } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';

import { PaginatedList, SearchForm } from 'components/common';

import LUTTableEntry from 'components/lookup-tables/LUTTableEntry';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

const LookupTablesOverview = React.createClass({

  mixins: [
    Reflux.connect(LookupTablesStore),
  ],

  getInitialState() {
    return {};
  },

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    LookupTablesActions.searchPaginated(this.state.pagination.page, this.state.pagination.per_page, this.state.pagination.query);
  },

  _onPageChange(newPage, newPerPage) {
    LookupTablesActions.searchPaginated(newPage, newPerPage, this.state.pagination.query);
  },

  _onSearch() {

  },

  _lookupName(id, map) {
    const empty = { title: 'None' };
    if (!map) {
      return empty;
    }
    return map[id] || empty;
  },

  render() {
    const lookupTables = this.state.tables.map((table) => {
      const cache = this._lookupName(table.cache_id, this.state.caches);
      const dataAdapter = this._lookupName(table.data_adapter_id, this.state.dataAdapters);

      return (<LUTTableEntry key={table.id} table={table} cache={cache} dataAdapter={dataAdapter} />);
    });

    return (<div>
      <Row className="content">
        <Col md={12}>
          <h2>
            Configured lookup tables
            <span>&nbsp;<small>{this.state.pagination.total} total</small></span>
          </h2>
          <PaginatedList onChange={this._onPageChange} totalItems={this.state.pagination.total}>
            <SearchForm onSearch={this._onSearch} />
            <Table condensed hover>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Name</th>
                  <th>Cache</th>
                  <th>Data Provider</th>
                </tr>
              </thead>
              {lookupTables}
            </Table>
          </PaginatedList>
        </Col>
      </Row>
    </div>);
  },
});

export default LookupTablesOverview;
