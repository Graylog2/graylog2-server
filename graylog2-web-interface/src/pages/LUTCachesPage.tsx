import React from 'react';

import { LinkContainer } from 'components/common/router';
import connect from 'stores/connect';
import { Col, Row, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Cache, CacheCreate, CacheForm, CachesOverview } from 'components/lookup-tables';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import { LookupTableCachesActions, LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';
import LUTPageNavigation from 'components/lookup-tables/LUTPageNavigation';
import withHistory from 'routing/withHistory';

const _saved = (history) => {
  history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW);
};

const _isCreating = ({ action }) => action === 'create';

const _validateCache = (adapter) => {
  LookupTableCachesActions.validate(adapter);
};

type LUTCachesPageProps = {
  cache?: any;
  validationErrors?: any;
  types?: any;
  caches?: any[];
  history: any;
  location: any;
  pagination: any;
  action?: string;
  paginationQueryParameter: any;
};

class LUTCachesPage extends React.Component<LUTCachesPageProps, {
  [key: string]: any;
}> {
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

  componentWillUnmount() {
    const { page, pageSize } = this.props.paginationQueryParameter;
    LookupTableCachesActions.searchPaginated(page, pageSize);
  }

  _loadData = (props) => {
    const { pagination } = props;
    const { page, pageSize } = this.props.paginationQueryParameter;

    if (props.params && props.params.cacheName) {
      LookupTableCachesActions.get(props.params.cacheName);
    } else if (_isCreating(props)) {
      LookupTableCachesActions.getTypes();
    } else {
      LookupTableCachesActions.searchPaginated(page, pageSize, pagination.query);
    }
  };

  render() {
    const {
      action,
      cache,
      validationErrors,
      types,
      caches,
      pagination,
      history,
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
                         saved={() => _saved(history)}
                         validate={_validateCache}
                         validationErrors={validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = <Cache cache={cache} />;
      }
    } else if (_isCreating(this.props)) {
      if (!types) {
        content = <Spinner text="Loading data cache types" />;
      } else {
        content = (
          <CacheCreate types={types}
                       saved={() => _saved(history)}
                       validate={_validateCache}
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
        <LUTPageNavigation />
        <PageHeader title="Caches for Lookup Tables"
                    actions={(
                      <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}>
                        <Button bsStyle="success" style={{ marginLeft: 5 }}>Create cache</Button>
                      </LinkContainer>
                    )}>
          <span>Caches provide the actual values for lookup tables</span>
        </PageHeader>
        {content}
      </DocumentTitle>
    );
  }
}

LUTCachesPage.defaultProps = {
  cache: null,
  validationErrors: {},
  types: null,
  caches: null,
  action: undefined,
};

export default connect(
  withHistory(withParams(withLocation(withPaginationQueryParameter(LUTCachesPage)))),
  { cachesStore: LookupTableCachesStore },
  ({ cachesStore, ...otherProps }) => ({
    ...otherProps,
    ...cachesStore,
  }),
);
