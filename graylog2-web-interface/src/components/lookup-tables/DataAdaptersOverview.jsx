import React, { PropTypes } from 'react';
import { Button, Row, Col, Table } from 'react-bootstrap';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';
import { LinkContainer } from 'react-router-bootstrap';

import { PaginatedList, SearchForm, Spinner } from 'components/common';

import DataAdapterTableEntry from 'components/lookup-tables/DataAdapterTableEntry';

import Styles from './Overview.css';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdaptersOverview = React.createClass({

  propTypes: {
    dataAdapters: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    errorStates: PropTypes.object.isRequired,
  },

  _onPageChange(newPage, newPerPage) {
    LookupTableDataAdaptersActions.searchPaginated(newPage, newPerPage,
      this.props.pagination.query);
  },

  _onSearch(query, resetLoadingStateCb) {
    LookupTableDataAdaptersActions
      .searchPaginated(this.props.pagination.page, this.props.pagination.per_page, query)
      .then(resetLoadingStateCb);
  },

  render() {
    if (!this.props.dataAdapters) {
      return <Spinner text="Loading data adapters" />;
    }
    const dataAdapters = this.props.dataAdapters.map((dataAdapter) => {
      return (<DataAdapterTableEntry key={dataAdapter.id}
                                     adapter={dataAdapter}
                                     error={this.props.errorStates.dataAdapters[dataAdapter.name]} />);
    });

    return (<div>
      <Row className="content">
        <Col md={12}>
          <h2>
            Configured lookup Data Adapters
            <span>&nbsp;
              <small>{this.props.pagination.total} total</small></span>
          </h2>
          <PaginatedList onChange={this._onPageChange} totalItems={this.props.pagination.total}>
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
