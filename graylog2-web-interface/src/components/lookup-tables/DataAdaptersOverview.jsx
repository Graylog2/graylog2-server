import React, { PropTypes } from 'react';
import { Button, Row, Col, Table, Popover, OverlayTrigger } from 'react-bootstrap';
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

  _onReset() {
    LookupTableDataAdaptersActions.searchPaginated(this.props.pagination.page, this.props.pagination.per_page);
  },

  _helpPopover() {
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
          <kbd>{'name:geoip'}</kbd><br />
          <kbd>{'name:geo'}</kbd>
        </p>
        <p>
          Searching without a field name matches against the <code>title</code> field:<br />
          <kbd>{'geoip'}</kbd> <br />is the same as<br />
          <kbd>{'title:geoip'}</kbd>
        </p>
      </Popover>
    );
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
            <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE}>
                <Button bsStyle="success" style={{ marginLeft: 5 }}>Create data adapter</Button>
              </LinkContainer>
              <OverlayTrigger trigger="click" rootClose placement="right" overlay={this._helpPopover()}>
                <Button bsStyle="link" className={Styles.searchHelpButton}><i className="fa fa-fw fa-question-circle" /></Button>
              </OverlayTrigger>
            </SearchForm>
            <Table condensed hover>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Name</th>
                  <th>Throughput</th>
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
