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

import { LookupTableForm } from 'components/lookup-tables';
import type { LookupTable } from 'logic/lookup-tables/types';

type LookupTableType = LookupTable & {
  enable_single_value: boolean;
  enable_multi_value: boolean;
};

type Props = {
  create: boolean;
  onClose: () => void;
  onCacheCreateClick: () => void;
  onDataAdapterCreateClick: () => void;
  dataAdapter?: string;
  cache?: string;
  table?: LookupTableType;
};

const LookupTableCreate = ({
  create,
  onClose,
  onCacheCreateClick,
  onDataAdapterCreateClick,
  dataAdapter = '',
  cache = '',
  table = undefined,
}: Props) => (
  <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
    <LookupTableForm
      onClose={onClose}
      onCacheCreateClick={onCacheCreateClick}
      onDataAdapterCreateClick={onDataAdapterCreateClick}
      create={create}
      dataAdapter={dataAdapter}
      cache={cache}
      table={table}
    />
  </div>
);

export default LookupTableCreate;
