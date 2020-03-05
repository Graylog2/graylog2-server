import _ from 'lodash';
import Immutable from 'immutable';

export default {
  convert(result) {
    const fieldNames = Immutable.Map().withMutations((map) => {
      _.forEach(result.messages, (msg) => {
        _.forOwn(msg.message, (value, field) => {
          // add occurrences
          map.mergeWith((oldVal, newVal) => oldVal + newVal, Immutable.Map([[field, 1]]));
        });
      });
    });

    return {
      id: result.id,
      effectiveTimerange: result.effective_timerange,
      type: result.type,
      sort: result.sort,
      messages: result.messages,
      total: result.total_results,
      fields: fieldNames, // computed fieldname -> occurrence count
    };
  },
};
