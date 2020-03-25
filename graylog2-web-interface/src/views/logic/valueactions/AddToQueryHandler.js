// @flow strict
import moment from 'moment-timezone';

import FieldType from 'views/logic/fieldtypes/FieldType';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import type { ActionHandler } from 'views/components/actions/ActionHandler';
import QueryManipulationHandler from './QueryManipulationHandler';

export default class AddToQueryHandler extends QueryManipulationHandler {
  formatTimestampForES = (value: string) => {
    const utc = moment(value).tz('UTC');
    return `"${utc.format('YYYY-MM-DD hh:mm:ss.SSS')}"`;
  };

  formatNewQuery = (oldQuery: string, field: string, value: string, type: FieldType) => {
    const predicateValue = type.type === 'date'
      ? this.formatTimestampForES(value)
      : escape(value);
    const fieldPredicate = `${field}:${predicateValue}`;

    return addToQuery(oldQuery, fieldPredicate);
  };

  handle: ActionHandler = ({ queryId, field, value = '', type }) => {
    const oldQuery = this.currentQueryString(queryId);
    const newQuery = this.formatNewQuery(oldQuery, field, value, type);

    return this.updateQueryString(queryId, newQuery);
  };
}
