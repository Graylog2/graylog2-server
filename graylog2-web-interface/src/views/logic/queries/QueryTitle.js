// @flow strict
import View from '../views/View';
import ViewState from '../views/ViewState';
import type { QueryId } from './Query';

const queryTitle = (view: View, queryId: QueryId): ?string => (view && view.search && view.search.queries
  ? view.search.queries.keySeq()
    .map((q, idx) => {
      if (queryId !== undefined && q.id !== undefined && queryId === q.id) {
        return view.state
          ? view.state.getIn([q.id], ViewState.create()).titles.getIn(['tab', 'title'], `Page#${idx + 1}`)
          : `Page#${idx + 1}`;
      }
      return undefined;
    }).filter((title) => title !== undefined)
    .first()
  : undefined);

export default queryTitle;
