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

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { OverlayTrigger, PaginatedList, SearchForm, Spinner, Icon } from 'components/common';
import { Row, Col, Table, Popover, Button } from 'components/bootstrap';
import CacheTableEntry from 'components/lookup-tables/CacheTableEntry';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { LookupTableCachesActions } from 'stores/lookup-tables/LookupTableCachesStore';

import Styles from './Overview.css';

const _helpPopover = () => {
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
            <td>Cache ID</td>
          </tr>
          <tr>
            <td>title</td>
            <td>The title of the cache</td>
          </tr>
          <tr>
            <td>name</td>
            <td>The reference name of the cache</td>
          </tr>
          <tr>
            <td>description</td>
            <td>The description of cache</td>
          </tr>
        </tbody>
      </Table>
      <p><strong>Examples</strong></p>
      <p>
        Find caches by parts of their names:<br />
        <kbd>name:guava</kbd><br />
        <kbd>name:gua</kbd>
      </p>
      <p>
        Searching without a field name matches against the <code>title</code> field:<br />
        <kbd>guava</kbd> <br />is the same as<br />
        <kbd>title:guava</kbd>
      </p>
    </Popover>
  );
};

class CachesOverview extends React.Component {
  static propTypes = {
    caches: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    paginationQueryParameter: PropTypes.object.isRequired,
  };

  _onPageChange = (newPage, newPerPage) => {
    const { pagination } = this.props;

    LookupTableCachesActions.searchPaginated(newPage, newPerPage, pagination.query);
  };

  _onSearch = (query, resetLoadingStateCb) => {
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    LookupTableCachesActions
      .searchPaginated(1, pageSize, query)
      .then(resetLoadingStateCb);
  };

  _onReset = () => {
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    LookupTableCachesActions.searchPaginated(1, pageSize);
  };

  render() {
    const { caches, pagination } = this.props;

    if (!caches) {
      return <Spinner text="Loading caches" />;
    }

    const cacheTableEntries = caches.map((cache) => {
      return (
        <CacheTableEntry key={cache.id}
                         cache={cache} />
      );
    });

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <h2>
              Configured lookup Caches
              <span>&nbsp;
                <small>{pagination.total} total</small>
              </span>
            </h2>
            <PaginatedList onChange={this._onPageChange} totalItems={pagination.total}>
              <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}>
                  <Button bsStyle="success" style={{ marginLeft: 5 }}>Create cache</Button>
                </LinkContainer>
                <OverlayTrigger trigger="click" rootClose placement="right" overlay={_helpPopover()}>
                  <Button bsStyle="link" className={Styles.searchHelpButton}><Icon name="question-circle" fixedWidth /></Button>
                </OverlayTrigger>
              </SearchForm>
              <Table condensed hover className={Styles.overviewTable}>
                <thead>
                  <tr>
                    <th className={Styles.rowTitle}>Title</th>
                    <th className={Styles.rowDescription}>Description</th>
                    <th className={Styles.rowName}>Name</th>
                    <th>Entries</th>
                    <th>Hit rate</th>
                    <th>Throughput</th>
                    <th className={Styles.rowActions}>Actions</th>
                  </tr>
                </thead>
                {cacheTableEntries}
              </Table>
            </PaginatedList>
          </Col>
        </Row>
      </div>
    );
  }
}

export default withPaginationQueryParameter(CachesOverview);
