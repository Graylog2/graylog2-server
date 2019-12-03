// @flow strict
import moment from 'moment-timezone';

import { QueriesActions } from 'views/stores/QueriesStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Query from 'views/logic/queries/Query';
import View from 'views/logic/views/View';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import type { ElasticsearchQueryString } from 'views/logic/queries/Query';
import type { ActionHandler } from 'views/components/actions/ActionHandler';
import QueryManipulationHandler from './QueryManipulationHandler';
import GlobalOverride from '../search/GlobalOverride';

export default class AddToQueryHandler extends QueryManipulationHandler {
  formatTimestampForES = (value: string) => {
    const utc = moment(value).tz('UTC');
    return `"${utc.format('YYYY-MM-DD hh:mm:ss.SSS')}"`;
  };

  formatNewQuery = (oldQuery: string, field: string, value: string, type: FieldType) => {
    let predicateValue;
    if (type.type === 'date') {
      predicateValue = this.formatTimestampForES(value);
    } else {
      predicateValue = escape(value);
    }
    const fieldPredicate = `${field}:${predicateValue}`;

    return addToQuery(oldQuery, fieldPredicate);
  };

  handle: ActionHandler = ({ queryId, field, value = '', type, contexts: { view } }) => {
    if (view.type === View.Type.Search) {
      const query: Query = this.queries.get(queryId);
      const oldQuery = query.query.query_string;
      const newQuery = this.formatNewQuery(oldQuery, field, value, type);
      return QueriesActions.query(queryId, newQuery);
    }

    const globalOverride: ?GlobalOverride = GlobalOverrideStore.getInitialState();
    const { query_string: oldQuery }: ElasticsearchQueryString = globalOverride && globalOverride.query
      ? globalOverride.query
      : { type: 'elasticsearch', query_string: '' };
    const newQuery = this.formatNewQuery(oldQuery, field, value, type);

    return GlobalOverrideActions.query(newQuery);
  };
}
