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

import connect from 'stores/connect';
import { Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { CachesOverview } from 'components/lookup-tables';
import type { PaginationProps } from 'components/common/withPaginationQueryParameter';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import type { ParamsContext } from 'routing/withParams';
import withParams from 'routing/withParams';
import useLocation from 'routing/useLocation';
import { LookupTableCachesActions, LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';
import LUTPageNavigation from 'components/lookup-tables/LUTPageNavigation';
import { useModalContext } from 'components/lookup-tables/LUTModals/ModalContext';
import LUTModals from 'components/lookup-tables/LUTModals';

type LUTCachesPageProps = ParamsContext &
  PaginationProps & {
    caches?: any[];
    action?: string;
  };

const LUTCachesPage = (props: LUTCachesPageProps) => {
  const {
    caches,
    pagination,
    paginationQueryParameter,
    params,
  } = props;

  const location = useLocation();
  const { setModal } = useModalContext();

  // Load data on mount and on pathname change
  React.useEffect(() => {
    loadData();

    return () => {
      const { page, pageSize } = paginationQueryParameter;
      LookupTableCachesActions.searchPaginated(page, pageSize);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname]);

  const isCreating = ({ action }: LUTCachesPageProps) => action === 'create';

  const loadData = () => {
    const { page, pageSize } = paginationQueryParameter;

    if (params && params.cacheName) {
      LookupTableCachesActions.get(params.cacheName);
    } else if (isCreating(props)) {
      LookupTableCachesActions.getTypes();
    } else {
      LookupTableCachesActions.searchPaginated(page, pageSize, pagination?.query);
    }
  };

  const showCreateModal = () => {
    setModal('CACHE-CREATE');
  };

  return (
    <DocumentTitle title="Lookup Tables - Caches">
      <LUTPageNavigation />
      <PageHeader
        title="Caches for Lookup Tables"
        actions={
          <Button
            bsStyle="success"
            style={{ marginLeft: 5 }}
            onClick={showCreateModal}>
            Create cache
          </Button>
        }>
        <span>Caches provide the actual values for lookup tables</span>
      </PageHeader>
      {!caches ? (
        <Spinner text="Loading caches" />
      ) : (
        <CachesOverview />
      )}
      <LUTModals />
    </DocumentTitle>
  );
};

LUTCachesPage.defaultProps = {
  cache: null,
  validationErrors: {},
  types: null,
  caches: null,
  action: undefined,
};

export default connect(
  withParams((withPaginationQueryParameter(LUTCachesPage))),
  { cachesStore: LookupTableCachesStore },
  ({ cachesStore, ...otherProps }) => ({
    ...otherProps,
    ...cachesStore,
  }),
);
