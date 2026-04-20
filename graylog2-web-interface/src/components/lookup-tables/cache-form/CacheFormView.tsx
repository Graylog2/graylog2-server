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
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { RowContainer } from 'components/lookup-tables/layout-componets';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import CacheForm from './CacheForm';
import CacheTypeSelect from './CacheTypeSelect';

const UseExistingButton = styled(Button)`
  margin-top: 1.57rem;
`;

type Props = {
  saved?: (adapterObj: LookupTableCache) => void;
  onCancel: () => void;
  cache?: LookupTableCache;
  isStep?: boolean;
};

function CacheFormView({ saved = undefined, onCancel, cache = undefined, isStep = false }: Props) {
  const [lutCache, setLutCache] = useState<LookupTableCache>(cache);
  const isCreate = useMemo(() => !lutCache?.id, [lutCache]);

  return (
    <>
      {isCreate && (
        <RowContainer>
          <CacheTypeSelect cacheConfigType={lutCache ? lutCache.config.type : null} onCacheChange={setLutCache} />
          {isStep && <UseExistingButton onClick={onCancel}>Use Existing Cache</UseExistingButton>}
        </RowContainer>
      )}
      {lutCache && (
        <CacheForm
          cache={lutCache}
          type={lutCache?.config?.type}
          create={isCreate}
          title="Configure Cache"
          saved={saved}
          onCancel={onCancel}
        />
      )}
    </>
  );
}

export default CacheFormView;
