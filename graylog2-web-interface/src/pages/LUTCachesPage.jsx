import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import connect from 'stores/connect';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import Routes from 'routing/Routes';
import history from 'util/History';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { Cache, CacheCreate, CacheForm, CachesOverview } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableCachesStore, LookupTableCachesActions } = CombinedProvider.get(
  'LookupTableCaches',
);

class LUTCachesPage extends React.Component {
  componentDidMount() {
    this._loadData(this.props);
  }

  componentDidUpdate(prevProps) {
    const { location: { pathname } } = this.props;
    const { location: { pathname: prevPathname } } = prevProps;
    if (pathname !== prevPathname) {
      this._loadData(this.props);
    }
  }

  _loadData = (props) => {
    const { pagination } = props;
    if (props.params && props.params.cacheName) {
      LookupTableCachesActions.get(props.params.cacheName);
    } else if (this._isCreating(props)) {
      LookupTableCachesActions.getTypes();
    } else {
      LookupTableCachesActions.searchPaginated(pagination.page, pagination.per_page, pagination.query);
    }
  };

  _saved = () => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW);
  }

  _isCreating = (props) => {
    return props.route.action === 'create';
  }

  _validateCache = (adapter) => {
    LookupTableCachesActions.validate(adapter);
  };

  render() {
    const {
      route: { action },
      cache,
      validationErrors,
      types,
      caches,
      pagination,
    } = this.props;
    let content;
    const isShowing = action === 'show';
    const isEditing = action === 'edit';

    if (isShowing || isEditing) {
      if (!cache) {
        content = <Spinner text="Loading data cache" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={12}>
              <CacheForm cache={cache}
                         type={cache.config.type}
                         title="Data Cache"
                         create={false}
                         saved={this._saved}
                         validate={this._validateCache}
                         validationErrors={validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = <Cache cache={cache} />;
      }
    } else if (this._isCreating(this.props)) {
      if (!types) {
        content = <Spinner text="Loading data cache types" />;
      } else {
        content = (
          <CacheCreate types={types}
                       saved={this._saved}
                       validate={this._validateCache}
                       validationErrors={validationErrors} />
        );
      }
    } else if (!caches) {
      content = <Spinner text="Loading caches" />;
    } else {
      content = (
        <CachesOverview caches={caches}
                        pagination={pagination} />
      );
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
  }
}
LUTCachesPage.propTypes = {
  cache: PropTypes.object,
  validationErrors: PropTypes.object,
  types: PropTypes.object,
  caches: PropTypes.array,
  location: PropTypes.object.isRequired,
  pagination: PropTypes.object.isRequired,
  route: PropTypes.object.isRequired,
};

LUTCachesPage.defaultProps = {
  cache: null,
  validationErrors: {},
  types: null,
  caches: null,
};
export default connect(LUTCachesPage, { cachesStore: LookupTableCachesStore }, ({ cachesStore, ...otherProps }) => ({
  ...otherProps,
  ...cachesStore,
}));
