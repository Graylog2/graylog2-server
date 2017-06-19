import React, { PropTypes } from 'react';
import { Button, Row, Col, Table, Popover, OverlayTrigger } from 'react-bootstrap';
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

  _onReset() {
    LookupTableCachesActions.searchPaginated(this.props.pagination.page, this.props.pagination.per_page);
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
          <kbd>{'name:guava'}</kbd><br />
          <kbd>{'name:gua'}</kbd>
        </p>
        <p>
          Searching without a field name matches against the <code>title</code> field:<br />
          <kbd>{'guava'}</kbd> <br />is the same as<br />
          <kbd>{'title:guava'}</kbd>
        </p>
      </Popover>
    );
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
            <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}>
                <Button bsStyle="success" style={{ marginLeft: 5 }}>Create cache</Button>
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
                  <th>Entries</th>
                  <th>Hit rate</th>
                  <th>Throughput</th>
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
