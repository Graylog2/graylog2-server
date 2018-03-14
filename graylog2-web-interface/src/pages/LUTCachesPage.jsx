import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';
import history from 'util/History';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { Cache, CacheCreate, CacheForm, CachesOverview } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableCachesStore, LookupTableCachesActions } = CombinedProvider.get(
  'LookupTableCaches');

const LUTCachesPage = createReactClass({
  displayName: 'LUTCachesPage',

  propTypes: {
    // eslint-disable-next-line react/no-unused-prop-types
    params: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTableCachesStore),
  ],

  componentDidMount() {
    this._loadData(this.props);
  },

  componentWillReceiveProps(nextProps) {
    this._loadData(nextProps);
  },

  _loadData(props) {
    if (props.params && props.params.cacheName) {
      LookupTableCachesActions.get(props.params.cacheName);
    } else if (this._isCreating(props)) {
      LookupTableCachesActions.getTypes();
    } else {
      const p = this.state.pagination;
      LookupTableCachesActions.searchPaginated(p.page, p.per_page, p.query);
    }
  },

  _saved() {
    // reset detail state
    this.setState({ cache: undefined });
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW);
  },

  _isCreating(props) {
    return props.route.action === 'create';
  },

  _validateCache(adapter) {
    LookupTableCachesActions.validate(adapter);
  },

  render() {
    let content;
    const isShowing = this.props.route.action === 'show';
    const isEditing = this.props.route.action === 'edit';

    if (isShowing || isEditing) {
      if (!this.state.cache) {
        content = <Spinner text="Loading data cache" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={12}>
              <h2>Data Cache</h2>
              <CacheForm cache={this.state.cache}
                         type={this.state.cache.config.type}
                         create={false}
                         saved={this._saved}
                         validate={this._validateCache}
                         validationErrors={this.state.validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = <Cache cache={this.state.cache} />;
      }
    } else if (this._isCreating(this.props)) {
      if (!this.state.types) {
        content = <Spinner text="Loading data cache types" />;
      } else {
        content =
          (<CacheCreate types={this.state.types}
                        saved={this._saved}
                        validate={this._validateCache}
                        validationErrors={this.state.validationErrors} />);
      }
    } else if (!this.state.caches) {
      content = <Spinner text="Loading caches" />;
    } else {
      content = (<CachesOverview caches={this.state.caches}
                                 pagination={this.state.pagination} />);
    }

    return (
      <DocumentTitle title="Lookup Tables - Caches">
        <span>
          <PageHeader title="Caches for Lookup Tables">
            <span>Caches provide the actual values for lookup tables</span>
            {null}
            <span>
              <ButtonToolbar>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW}>
                  <Button bsStyle="info">Lookup Tables</Button>
                </LinkContainer>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW}>
                  <Button bsStyle="info" className="active">Caches</Button>
                </LinkContainer>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}>
                  <Button bsStyle="info">Data Adapters</Button>
                </LinkContainer>
              </ButtonToolbar>
            </span>
          </PageHeader>

          {content}
        </span>
      </DocumentTitle>
    );
  },
});

export default LUTCachesPage;
