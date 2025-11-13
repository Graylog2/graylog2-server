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

import { Row, Col } from 'components/bootstrap';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import CacheForm from './CacheForm';
import CacheTypeSelect from './CacheTypeSelect';

const StyledRow = styled(Row)`
  display: flex;
  justify-content: center;
`;

const FlexCol = styled(Col)`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

type Props = {
  saved?: (adapterObj: LookupTableCache) => void;
  onCancel: () => void;
  cache?: LookupTableCache;
};

function CacheFormView({ saved = undefined, onCancel, cache = undefined }: Props) {
  const [lutCache, setLutCache] = React.useState<LookupTableCache>(cache);
  const isCreate = React.useMemo(() => !lutCache?.id, [lutCache]);

  return (
    <>
      {isCreate && (
        <StyledRow>
          <Col lg={6}>
            <CacheTypeSelect cacheConfigType={lutCache ? lutCache.config.type : null} onCacheChange={setLutCache} />
          </Col>
        </StyledRow>
      )}
      {lutCache && (
        <StyledRow style={{ flexGrow: 1 }}>
          <FlexCol lg={9}>
            <CacheForm
              cache={lutCache}
              type={lutCache?.config?.type}
              create={isCreate}
              title="Configure Cache"
              saved={saved}
              onCancel={onCancel}
            />
          </FlexCol>
        </StyledRow>
      )}
    </>
  );
}

export default CacheFormView;
