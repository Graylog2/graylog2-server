import React from 'react';
import Reflux from 'reflux';
import { Button, Row, Col, Table } from 'react-bootstrap';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';
import { LinkContainer } from 'react-router-bootstrap';

import { PaginatedList, SearchForm } from 'components/common';

import DataAdapterTableEntry from 'components/lookup-tables/DataAdapterTableEntry';

import Styles from './DataAdaptersOverview.css';

const { LookupTableDataAdaptersStore, LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdaptersOverview = React.createClass({

  mixins: [
    Reflux.connect(LookupTableDataAdaptersStore),
  ],

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    LookupTableDataAdaptersActions.searchPaginated(this.state.pagination.page, this.state.pagination.per_page, this.state.pagination.query);
  },

  _onPageChange(newPage, newPerPage) {
    LookupTableDataAdaptersActions.searchPaginated(newPage, newPerPage, this.state.pagination.query);
  },

  _onSearch() {

  },

  render() {
    const dataAdapters = this.state.dataAdapters.map((dataAdapter) => {
      return (<DataAdapterTableEntry key={dataAdapter.id} adapter={dataAdapter} refresh={this.loadData}/>);
    });

    return (<div>
      <Row className="content">
        <Col md={12}>
          <h2>
            Configured lookup dataAdapters
            <span>&nbsp;<small>{this.state.pagination.total} total</small></span>
          </h2>
          <PaginatedList onChange={this._onPageChange} totalItems={this.state.pagination.total}>
            <SearchForm onSearch={this._onSearch}>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE}>
                <Button bsStyle="success" style={{ marginLeft: 5 }}>Create data adapter</Button>
              </LinkContainer>
            </SearchForm>
            <Table condensed hover>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Name</th>
                  <th>Entries</th>
                  <th className={Styles.actions}>Actions</th>
                </tr>
              </thead>
              {dataAdapters}
            </Table>
          </PaginatedList>
        </Col>
      </Row>
    </div>);
  },
});

export default DataAdaptersOverview;
