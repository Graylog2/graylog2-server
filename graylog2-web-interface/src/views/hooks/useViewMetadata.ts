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
import { createSelector } from '@reduxjs/toolkit';

import useAppSelector from 'stores/useAppSelector';
import { selectActiveQuery, selectView } from 'views/logic/slices/viewSelectors';
import type View from 'views/logic/views/View';

const selectViewMetadata = createSelector(selectActiveQuery, selectView, (activeQuery: string, view: View) => (view
  ? {
    id: view?.id,
    title: view?.title,
    description: view?.description,
    summary: view?.summary,
    activeQuery,
  }
  : {}));

const useViewMetadata = () => useAppSelector(selectViewMetadata);

export default useViewMetadata;
