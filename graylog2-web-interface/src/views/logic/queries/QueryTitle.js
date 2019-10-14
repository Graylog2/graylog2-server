// @flow strict

import View from '../views/View';
import ViewState from '../views/ViewState';
import Query from './Query';

const queryTitle = (view: View, query: Query): string => view.search.queries.keySeq()
  .map((q, idx) => {
    if (query.id !== undefined && q.id !== undefined && query.id === q.id) {
      return view.state
        ? view.state.getIn([q.id], ViewState.create()).titles.getIn(['tab', 'title'], `Query#${idx + 1}`)
        : `Query#${idx + 1}`;
    }
    return undefined;
  }).filter(title => title !== undefined)
  .first();

export default queryTitle;
