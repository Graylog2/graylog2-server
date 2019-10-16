// @flow strict

import View from '../views/View';
import ViewState from '../views/ViewState';
import Query from './Query';
import type { QueryId } from './Query';

const queryTitle = (view: View, queryId: QueryId): string => view.search.queries.keySeq()
  .map((q, idx) => {
    if (queryId !== undefined && q.id !== undefined && queryId === q.id) {
      return view.state
        ? view.state.getIn([q.id], ViewState.create()).titles.getIn(['tab', 'title'], `Query#${idx + 1}`)
        : `Query#${idx + 1}`;
    }
    return undefined;
  }).filter(title => title !== undefined)
  .first();

export default queryTitle;
