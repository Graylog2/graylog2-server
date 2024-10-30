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
import React from 'react';

import { LinkContainer } from 'components/common/router';
import connect from 'stores/connect';
import { Col, Row, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Cache, CacheCreate, CacheForm, CachesOverview } from 'components/lookup-tables';
import type { PaginationProps } from 'components/common/withPaginationQueryParameter';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import type { ParamsContext } from 'routing/withParams';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import { LookupTableCachesActions, LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';
import LUTPageNavigation from 'components/lookup-tables/LUTPageNavigation';
import withHistory from 'routing/withHistory';

const _saved = (history) => {
  history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW);
};

const _isCreating = ({ action }: LUTCachesPageProps) => action === 'create';

const _validateCache = (adapter) => {
  LookupTableCachesActions.validate(adapter);
};

type LUTCachesPageProps = ParamsContext & PaginationProps & {
  cache?: any;
  validationErrors?: any;
  types?: any;
  caches?: any[];
  history: any;
  location: any;
  pagination: any;
  action?: string;
};

class LUTCachesPage extends React.Component<LUTCachesPageProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    cache: null,
    validationErrors: {},
    types: null,
    caches: null,
    action: undefined,
  };

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

export default connect(
  withHistory(withParams(withLocation(withPaginationQueryParameter(LUTCachesPage)))),
  { cachesStore: LookupTableCachesStore },
  ({ cachesStore, ...otherProps }) => ({
    ...otherProps,
    ...cachesStore,
  }),
);
