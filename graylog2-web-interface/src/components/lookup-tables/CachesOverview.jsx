import React, { PropTypes } from 'react';
import { Button, Row, Col, Table } from 'react-bootstrap';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';
import { LinkContainer } from 'react-router-bootstrap';

import { PaginatedList, SearchForm, Spinner } from 'components/common';

import CacheTableEntry from 'components/lookup-tables/CacheTableEntry';

import Styles from './Overview.css';

const { LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

const CachesOverview = React.createClass({

  propTypes: {
    caches: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
  },

  _onPageChange(newPage, newPerPage) {
    LookupTableCachesActions.searchPaginated(newPage, newPerPage, this.props.pagination.query);
  },

  _onSearch(query, resetLoadingStateCb) {
    LookupTableCachesActions
      .searchPaginated(this.props.pagination.page, this.props.pagination.per_page, query)
      .then(resetLoadingStateCb);
  },

  render() {
    if (!this.props.caches) {
      return <Spinner text="Loading caches" />;
    }
    const caches = this.props.caches.map((cache) => {
      return (<CacheTableEntry key={cache.id}
                               cache={cache} />);
    });

    return (<div>
      <Row className="content">
        <Col md={12}>
          <h2>
            Configured lookup Caches
            <span>&nbsp;
              <small>{this.props.pagination.total} total</small></span>
          </h2>
          <PaginatedList onChange={this._onPageChange} totalItems={this.props.pagination.total}>
            <SearchForm onSearch={this._onSearch}>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}>
                <Button bsStyle="success" style={{ marginLeft: 5 }}>Create cache</Button>
              </LinkContainer>
            </SearchForm>
            <Table condensed hover>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Name</th>
                  <th>Entries</th>
                  <th>Hit rate</th>
                  <th className={Styles.actions}>Actions</th>
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

export default CachesOverview;
