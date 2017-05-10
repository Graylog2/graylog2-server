import React, { PropTypes } from 'react';
import { Button, Row, Col, Table } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';

import { PaginatedList, SearchForm } from 'components/common';
import LUTTableEntry from 'components/lookup-tables/LUTTableEntry';

import Styles from './Overview.css';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

const LookupTablesOverview = React.createClass({

  propTypes: {
    tables: PropTypes.arrayOf(PropTypes.object).isRequired,
    caches: PropTypes.objectOf(PropTypes.object).isRequired,
    dataAdapters: PropTypes.objectOf(PropTypes.object).isRequired,
    pagination: PropTypes.object.isRequired,
    errorStates: PropTypes.object.isRequired,
  },

  _onPageChange(newPage, newPerPage) {
    LookupTablesActions.searchPaginated(newPage, newPerPage, this.props.pagination.query);
  },

  _onSearch(query, resetLoadingStateCb) {
    LookupTablesActions
      .searchPaginated(this.props.pagination.page, this.props.pagination.per_page, query)
      .then(resetLoadingStateCb);
  },

  _lookupName(id, map) {
    const empty = { title: 'None' };
    if (!map) {
      return empty;
    }
    return map[id] || empty;
  },

  _lookupAdapterError(table) {
    if (this.props.errorStates.dataAdapters && this.props.dataAdapters) {
      const adapter = this.props.dataAdapters[table.data_adapter_id];
      if (!adapter) {
        return null;
      }
      return this.props.errorStates.dataAdapters[adapter.name];
    }
    return null;
  },

  render() {
    const lookupTables = this.props.tables.map((table) => {
      const cache = this._lookupName(table.cache_id, this.props.caches);
      const dataAdapter = this._lookupName(table.data_adapter_id, this.props.dataAdapters);
      const errors = {
        table: this.props.errorStates.tables[table.name],
        cache: null,
        dataAdapter: this._lookupAdapterError(table),
      };

      return (<LUTTableEntry key={table.id}
                             table={table}
                             cache={cache}
                             dataAdapter={dataAdapter}
                             errors={errors} />);
    });

    return (<div>
      <Row className="content">
        <Col md={12}>
          <h2>
            Configured lookup tables
            <span>&nbsp;<small>{this.props.pagination.total} total</small></span>
          </h2>
          <PaginatedList onChange={this._onPageChange} totalItems={this.props.pagination.total}>
            <SearchForm onSearch={this._onSearch}>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CREATE}>
                <Button bsStyle="success" style={{ marginLeft: 5 }}>Create lookup table</Button>
              </LinkContainer>
            </SearchForm>
            <Table condensed hover>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Name</th>
                  <th>Cache</th>
                  <th>Data Adapter</th>
                  <th className={Styles.actions}>Actions</th>
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
