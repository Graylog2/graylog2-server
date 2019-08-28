// @flow strict

import View from 'views/logic/views/View';
import type { ViewJson } from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import DashboardState from '../logic/views/DashboardState';

const simpleView = (): View => View.builder()
  .id('foo')
  .title('Foo')
  .summary('summary')
  .description('Foo')
  .search(Search.create().toBuilder().id('foosearch').build())
  .dashboardState(DashboardState.create())
  .properties({})
  .state({})
  .createdAt(new Date())
  .owner('admin')
  .requires({})
  .build();

const simpleViewJson = (): ViewJson => ({ ...(simpleView().toJSON()), requires: {} });

export {
  simpleView,
  simpleViewJson,
};
