import type { PluginExports } from 'graylog-web-plugin/plugin';

import viewSliceReducer from 'views/logic/slices/viewSlice';

const viewsReducers: PluginExports['views.reducers'] = [{
  key: 'view',
  reducer: viewSliceReducer,
}];

export default viewsReducers;
