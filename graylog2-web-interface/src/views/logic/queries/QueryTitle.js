// @flow strict

import View from '../views/View';
import Query from './Query';

const queryTitle = (view: View, query: Query): string => {
  view.search.queries.keySeq().map((q, idx) => {
    
  }
    view.state ? view.state.getIn([q.id]).titles.getIn(['tab', 'title'], `Query#${idx + 1}`) : `Query#${idx + 1}`,
  ]).toJS();
};

export default queryTitle;
