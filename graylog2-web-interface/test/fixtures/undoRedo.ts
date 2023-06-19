import * as Immutable from 'immutable';

import type { UndoRedoState } from 'views/logic/slices/undoRedoSlice';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';

const getViewWithTitle = (title: string, idPrefix: string) => View.builder()
  .id(`foo-${idPrefix}`)
  .type(View.Type.Dashboard)
  .title(title)
  .summary('summary')
  .description('Foo')
  .search(Search.create().toBuilder().id(`foosearch-${idPrefix}`).build())
  .properties(Immutable.List())
  .state(Immutable.Map())
  .createdAt(new Date())
  .owner('admin')
  .requires({})
  .build();

export const testView1 = getViewWithTitle('View test title 1', 'view-1');
export const testView2 = getViewWithTitle('View test title 2', 'view-2');
export const testView3 = getViewWithTitle('View test title 3', 'view-3');
export const undoRedoTestStore: UndoRedoState = {
  buffer: [{
    type: 'view',
    state: {
      activeQuery: 'query-id-1',
      view: testView1,
      isDirty: false,
      isNew: false,
    },
  },
  {
    type: 'view',
    state: {
      activeQuery: 'query-id-1',
      view: testView2,
      isDirty: false,
      isNew: false,
    },
  },
  {
    type: 'view',
    state: {
      activeQuery: 'query-id-1',
      view: testView3,
      isDirty: false,
      isNew: false,
    },
  },
  ],
  currentRevision: 1,
};
