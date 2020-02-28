import PropTypes from 'prop-types';
import React from 'react';
import Routes from 'routing/Routes';
import { LinkContainer } from 'react-router-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
import { Row, Col, Table, Popover, OverlayTrigger, Button } from 'components/graylog';
import { PaginatedList, SearchForm, Spinner, Icon } from 'components/common';
import CacheTableEntry from 'components/lookup-tables/CacheTableEntry';

import Styles from './Overview.css';

const { LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

class CachesOverview extends React.Component {
  static propTypes = {
    caches: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
  };

  _onPageChange = (newPage, newPerPage) => {
    const { pagination } = this.props;

    LookupTableCachesActions.searchPaginated(newPage, newPerPage, pagination.query);
  };

  _onSearch = (query, resetLoadingStateCb) => {
    const { pagination } = this.props;

    LookupTableCachesActions
      .searchPaginated(pagination.page, pagination.per_page, query)
      .then(resetLoadingStateCb);
  };

  _onReset = () => {
    const { pagination } = this.props;

    LookupTableCachesActions.searchPaginated(pagination.page, pagination.per_page);
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

export default CachesOverview;
