// @flow strict
import moment from 'moment-timezone';

import { QueriesActions } from 'views/stores/QueriesStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Query from 'views/logic/queries/Query';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import QueryManipulationHandler from './QueryManipulationHandler';
import type { ValueActionHandler } from './ValueActionHandler';

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

  handle: ValueActionHandler = (queryId: string, field: string, value: string, type: FieldType) => {
    const query: Query = this.queries.get(queryId);
    const oldQuery = query.query.query_string;
    const newQuery = this.formatNewQuery(oldQuery, field, value, type);
    return QueriesActions.query(queryId, newQuery);
  };
}
