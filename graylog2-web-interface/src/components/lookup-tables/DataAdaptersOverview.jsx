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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Table, Popover, OverlayTrigger, Button } from 'components/graylog';
import { PaginatedList, SearchForm, Spinner, Icon } from 'components/common';
import DataAdapterTableEntry from 'components/lookup-tables/DataAdapterTableEntry';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import Styles from './Overview.css';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

class DataAdaptersOverview extends React.Component {
  static propTypes = {
    dataAdapters: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    errorStates: PropTypes.object.isRequired,
  };

  _onPageChange = (newPage, newPerPage) => {
    const { pagination } = this.props;

    LookupTableDataAdaptersActions.searchPaginated(newPage, newPerPage, pagination.query);
  };

  _onSearch = (query, resetLoadingStateCb) => {
    const { pagination } = this.props;

    LookupTableDataAdaptersActions
      .searchPaginated(pagination.page, pagination.per_page, query)
      .then(resetLoadingStateCb);
  };

  _onReset = () => {
    const { pagination } = this.props;

    LookupTableDataAdaptersActions.searchPaginated(pagination.page, pagination.per_page);
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

  render() {
    const { dataAdapters, errorStates, pagination } = this.props;

    if (!dataAdapters) {
      return <Spinner text="Loading data adapters" />;
    }

    const dataAdapterEntries = dataAdapters.map((dataAdapter) => {
      return (
        <DataAdapterTableEntry key={dataAdapter.id}
                               adapter={dataAdapter}
                               error={errorStates.dataAdapters[dataAdapter.name]} />
      );
    });

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <h2>
              Configured lookup Data Adapters
              <span>&nbsp;
                <small>{pagination.total} total</small>
              </span>
            </h2>
            <PaginatedList onChange={this._onPageChange} totalItems={pagination.total}>
              <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE}>
                  <Button bsStyle="success" style={{ marginLeft: 5 }}>Create data adapter</Button>
                </LinkContainer>
                <OverlayTrigger trigger="click" rootClose placement="right" overlay={this._helpPopover()}>
                  <Button bsStyle="link" className={Styles.searchHelpButton}><Icon name="question-circle" fixedWidth /></Button>
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
                {dataAdapterEntries}
              </Table>
            </PaginatedList>
          </Col>
        </Row>
      </div>
    );
  }
}

export default DataAdaptersOverview;
