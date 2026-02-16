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
import { useMemo, useState } from 'react';
import { useFormikContext } from 'formik';

import { Spinner } from 'components/common';
import { RowContainer, ColContainer } from 'components/lookup-tables/layout-componets';
import useScopePermissions from 'hooks/useScopePermissions';
import { useFetchCache, useFetchAllCaches } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import CachePicker from 'components/lookup-tables/cache-form/CachePicker';
import CacheFormView from 'components/lookup-tables/cache-form/CacheFormView';
import type { LookupTable, LookupTableCache } from 'logic/lookup-tables/types';
import CacheShow from 'components/lookup-tables/cache-view/cache-show';

function CacheFormStep() {
  const { values, setFieldValue } = useFormikContext<LookupTable>();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(values);
  const { allCaches, loadingAllCaches } = useFetchAllCaches();
  const { cache, loadingCache } = useFetchCache(values.cache_id);
  const [showForm, setShowForm] = useState<boolean>(false);
  const showCache = useMemo(() => !!cache, [cache]);

  const canModify = useMemo(
    () => !values.id || (!loadingScopePermissions && scopePermissions?.is_mutable),
    [values.id, loadingScopePermissions, scopePermissions?.is_mutable],
  );

  const onSaved = (newCache: LookupTableCache) => {
    setFieldValue('cache_id', newCache.id);
    setShowForm(false);
  };

  const onCancel = () => {
    setFieldValue('cache_id', '');
    setShowForm(false);
  };

  const onCreateClick = () => {
    onCancel();
    setTimeout(() => setShowForm(true), 100);
  };

  return (
    <ColContainer $gap="lg" $align="center">
      {loadingAllCaches ? (
        <RowContainer>
          <Spinner text="Loading caches..." />
        </RowContainer>
      ) : (
        <>
          {canModify && !showForm && (
            <RowContainer>
              <CachePicker onCreateClick={onCreateClick} caches={allCaches} />
            </RowContainer>
          )}
          {showCache && !loadingCache && <CacheShow cache={cache} />}
          {showForm && !showCache && <CacheFormView onCancel={onCancel} saved={onSaved} isStep />}
        </>
      )}
    </ColContainer>
  );
}

export default CacheFormStep;
