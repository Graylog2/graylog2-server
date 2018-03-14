import PropTypes from 'prop-types';
import React from 'react';
import { Button, Row, Col, Table, Popover, OverlayTrigger } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';

import { PaginatedList, SearchForm } from 'components/common';
import LUTTableEntry from 'components/lookup-tables/LUTTableEntry';

import Styles from './Overview.css';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

class LookupTablesOverview extends React.Component {
  static propTypes = {
    tables: PropTypes.arrayOf(PropTypes.object).isRequired,
    caches: PropTypes.objectOf(PropTypes.object).isRequired,
    dataAdapters: PropTypes.objectOf(PropTypes.object).isRequired,
    pagination: PropTypes.object.isRequired,
    errorStates: PropTypes.object.isRequired,
  };

  _onPageChange = (newPage, newPerPage) => {
    LookupTablesActions.searchPaginated(newPage, newPerPage, this.props.pagination.query);
  };

  _onSearch = (query, resetLoadingStateCb) => {
    LookupTablesActions
      .searchPaginated(this.props.pagination.page, this.props.pagination.per_page, query)
      .then(resetLoadingStateCb);
  };

  _onReset = () => {
    LookupTablesActions.searchPaginated(this.props.pagination.page, this.props.pagination.per_page);
  };

  _lookupName = (id, map) => {
    const empty = { title: 'None' };
    if (!map) {
      return empty;
    }
    return map[id] || empty;
  };

  _lookupAdapterError = (table) => {
    if (this.props.errorStates.dataAdapters && this.props.dataAdapters) {
      const adapter = this.props.dataAdapters[table.data_adapter_id];
      if (!adapter) {
        return null;
      }
      return this.props.errorStates.dataAdapters[adapter.name];
    }
    return null;
  };

  _helpPopover = () => {
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
  };

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
            <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CREATE}>
                <Button bsStyle="success" style={{ marginLeft: 5 }}>Create lookup table</Button>
              </LinkContainer>
              <OverlayTrigger trigger="click" rootClose placement="right" overlay={this._helpPopover()}>
                <Button bsStyle="link" className={Styles.searchHelpButton}><i className="fa fa-fw fa-question-circle" /></Button>
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
              {lookupTables}
            </Table>
          </PaginatedList>
        </Col>
      </Row>
    </div>);
  }
}

export default LookupTablesOverview;
