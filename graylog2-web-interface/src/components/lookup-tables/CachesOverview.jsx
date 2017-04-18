import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Table } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';

import { PaginatedList, SearchForm } from 'components/common';

import CacheTableEntry from 'components/lookup-tables/CacheTableEntry';

const { LookupTableCachesStore, LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

const LookupTablesOverview = React.createClass({

  mixins: [
    Reflux.connect(LookupTableCachesStore),
  ],

  getInitialState() {
    return {};
  },

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    LookupTableCachesActions.searchPaginated(this.state.pagination.page, this.state.pagination.per_page, this.state.pagination.query);
  },

  _onPageChange(newPage, newPerPage) {
    LookupTableCachesActions.searchPaginated(newPage, newPerPage, this.state.pagination.query);
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
    const caches = this.state.caches.map((cache) => {
      return (<CacheTableEntry key={cache.id} cache={cache} />);
    });

    return (<div>
      <Row className="content">
        <Col md={12}>
          <h2>
            Configured lookup caches
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
                  <th>Entries</th>
                  <th>Hit rate</th>
                </tr>
              </thead>
              {caches}
            </Table>
          </PaginatedList>
        </Col>
      </Row>
    </div>);
  },
});

export default LookupTablesOverview;
