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
import { useMemo } from 'react';

import { RowContainer } from 'components/lookup-tables/layout-componets';
import usePluginEntities from 'hooks/usePluginEntities';
import Cache from 'components/lookup-tables/Cache';
import type { LookupTableCache } from 'logic/lookup-tables/types';

function CacheShow({ cache }: { cache: LookupTableCache }) {
  const plugins = usePluginEntities('lookupTableCaches');
  const cachePlugin = useMemo(
    () => plugins.find((p: any) => p.type === cache?.config?.type),
    [cache?.config?.type, plugins],
  );

  const DocComponent = useMemo(() => cachePlugin?.documentationComponent, [cachePlugin]);

  return (
    <RowContainer $gap="xl" $withDocs={!!DocComponent} $justify="center">
      <div style={{ width: DocComponent ? '50%' : '100%' }}>
        <Cache cache={cache} noEdit />
      </div>
      {DocComponent && (
        <div style={{ width: '50%' }}>
          <DocComponent cacheId={cache?.id} />
        </div>
      )}
    </RowContainer>
  );
}

export default CacheShow;
