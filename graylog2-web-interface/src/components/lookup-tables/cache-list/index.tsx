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
import * as React from 'react';

import { Row, Col } from 'components/bootstrap';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import QueryHelper from 'components/common/QueryHelper';
import { ErrorsProvider } from 'components/lookup-tables/contexts/ErrorsContext';
import { useFetchCaches } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { ModalProvider } from 'components/lookup-tables/contexts/ModalContext';
import LUTModals from 'components/lookup-tables/LUTModals';
import type { CacheEntity } from 'components/lookup-tables/types';
import type { SearchParams, Attribute } from 'stores/PaginationTypes';

import { cacheListElements } from './constants';
import columnRenderers from './column-renderers';
import useActions from './use-actions';

const queryHelpComponent = (
  <QueryHelper
    entityName="cache"
    commonFields={['id', 'title', 'name', 'description']}
    example={
      <p>
        searching without a field name matches against the <code>title</code> field:
        <br />
        <kbd>geoip</kbd> <br />
        is the same as
        <br />
        <kbd>title:geoip</kbd>
      </p>
    }
  />
);

function CacheList() {
  const { fetchPaginatedCaches, cachesKeyFn } = useFetchCaches();
  const { renderActions } = useActions();

  const handleFetchCaches = React.useCallback(
    async (searchParams: SearchParams) => {
      const resp = await fetchPaginatedCaches(searchParams);

      const overrides: Record<string, Partial<Attribute>> = {
        title: { sortable: true },
        name: { sortable: true },
        description: { sortable: false },
      };

      const expectedIds = cacheListElements.defaultLayout.defaultDisplayedAttributes;

      const attrMap = new Map<string, Attribute>();

      (resp.attributes ?? []).forEach((attr) => {
        const override = overrides[attr.id] ?? {};
        attrMap.set(attr.id, { ...attr, ...override });
      });

      expectedIds.forEach((id) => {
        if (!attrMap.has(id)) {
          const override = overrides[id] ?? {};
          attrMap.set(id, {
            id,
            title: id.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()),
            type: 'STRING',
            ...override,
          });
        }
      });

      return Promise.resolve({
        ...resp,
        attributes: Array.from(attrMap.values()),
      });
    },
    [fetchPaginatedCaches],
  );

  return (
    <ModalProvider>
      <ErrorsProvider>
        <Row className="content">
          <Col md={12}>
            <PaginatedEntityTable<CacheEntity>
              humanName="caches"
              entityActions={renderActions}
              columnsOrder={cacheListElements.columnOrder}
              queryHelpComponent={queryHelpComponent}
              tableLayout={cacheListElements.defaultLayout}
              fetchEntities={handleFetchCaches}
              keyFn={cachesKeyFn}
              actionsCellWidth={100}
              entityAttributesAreCamelCase={false}
              columnRenderers={columnRenderers}
            />
          </Col>
        </Row>
        <LUTModals />
      </ErrorsProvider>
    </ModalProvider>
  );
}

export default CacheList;
