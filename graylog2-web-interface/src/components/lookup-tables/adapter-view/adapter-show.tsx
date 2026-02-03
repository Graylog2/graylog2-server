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
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

function AdapterShow({ dataAdapter }: { dataAdapter: LookupTableAdapter }) {
  const plugins = usePluginEntities('lookupTableAdapters');
  const adapterPlugin = useMemo(
    () => plugins.find((p: any) => p.type === dataAdapter?.config?.type),
    [dataAdapter?.config?.type, plugins],
  );

  const DocComponent = useMemo(() => adapterPlugin?.documentationComponent, [adapterPlugin]);

  return (
    <RowContainer $gap="xl" $withDocs={!!DocComponent} $justify="center">
      <div style={{ width: DocComponent ? '50%' : '100%' }}>
        <DataAdapter dataAdapter={dataAdapter} noEdit />
      </div>
      {DocComponent && (
        <div style={{ width: '50%' }}>
          <DocComponent dataAdapterId={dataAdapter?.id} />
        </div>
      )}
    </RowContainer>
  );
}

export default AdapterShow;
