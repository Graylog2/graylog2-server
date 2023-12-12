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
import type { PluginExports } from 'graylog-web-plugin/plugin';

import { viewSliceReducer } from 'views/logic/slices/viewSlice';
import { searchExecutionSliceReducer } from 'views/logic/slices/searchExecutionSlice';
import { searchMetadataSliceReducer } from 'views/logic/slices/searchMetadataSlice';
import { undoRedoSliceReducer } from 'views/logic/slices/undoRedoSlice';

const viewsReducers: PluginExports['views.reducers'] = [{
  key: 'view',
  reducer: viewSliceReducer,
}, {
  key: 'searchExecution',
  reducer: searchExecutionSliceReducer,
}, {
  key: 'searchMetadata',
  reducer: searchMetadataSliceReducer,
}, {
  key: 'undoRedo',
  reducer: undoRedoSliceReducer,
},
];

export default viewsReducers;
