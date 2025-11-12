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
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import { useFetchCache, useFetchAllCaches } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import Cache from 'components/lookup-tables/Cache';
import type { LookupTable, LookupTableCache } from 'logic/lookup-tables/types';

import CachePicker from './CachePicker';
import CacheFormView from './CacheFormView';

const FlexCol = styled(Col)`
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 2rem;
`;

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

function CacheReadOnly({ cache }: { cache: LookupTableCache }) {
  const plugins = usePluginEntities('lookupTableCaches');
  const cachePlugin = React.useMemo(
    () => plugins.find((p: any) => p.type === cache?.config?.type),
    [cache?.config?.type, plugins],
  );

  const DocComponent = React.useMemo(() => cachePlugin?.documentationComponent, [cachePlugin]);

  return (
    <StyledRow>
      <Col lg={9}>
        <Col lg={6}>
          <Cache cache={cache} noEdit />
        </Col>
        <Col lg={6}>{DocComponent ? <DocComponent cacheId={cache?.id} /> : null}</Col>
      </Col>
    </StyledRow>
  );
}

function CacheFormStep() {
  const { values, setFieldValue } = useFormikContext<LookupTable>();
  const { allCaches, loadingAllCaches } = useFetchAllCaches();
  const { cache, loadingCache } = useFetchCache(values.cache_id);
  const [showForm, setShowForm] = React.useState<boolean>(false);
  const showCache = React.useMemo(() => values.cache_id, [values.cache_id]);

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
    <Row className="content" style={{ flexGrow: 1 }}>
      <FlexCol md={12}>
        {loadingAllCaches ? (
          <Spinner text="Loading caches..." />
        ) : (
          <>
            <StyledRow>
              <Col lg={6}>
                <CachePicker onCreateClick={onCreateClick} caches={allCaches} />
              </Col>
            </StyledRow>
            {showCache && !loadingCache && <CacheReadOnly cache={cache} />}
            {showForm && !showCache && <CacheFormView onCancel={onCancel} saved={onSaved} />}
          </>
        )}
      </FlexCol>
    </Row>
  );
}

export default CacheFormStep;
