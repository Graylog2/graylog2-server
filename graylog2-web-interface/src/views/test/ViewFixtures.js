// @flow strict
import * as Immutable from 'immutable';

import View from 'views/logic/views/View';
import type { ViewJson } from 'views/logic/views/View';
import Search from 'views/logic/search/Search';

const simpleView = (): View => View.builder()
  .id('foo')
  .type(View.Type.Dashboard)
  .title('Foo')
  .summary('summary')
  .description('Foo')
  .search(Search.create().toBuilder().id('foosearch').build())
  .properties(Immutable.List())
  .state(Immutable.Map())
  .createdAt(new Date())
  .owner('admin')
  .requires({})
  .build();

const simpleViewJson = (): ViewJson => ({ ...(simpleView().toJSON()), requires: {} });

export {
  simpleView,
  simpleViewJson,
};
