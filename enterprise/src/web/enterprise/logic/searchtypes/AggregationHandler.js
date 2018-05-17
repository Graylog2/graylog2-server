import Immutable from 'immutable';
import { get } from 'lodash';

export default {
  convert(result) {
    const aggregationResults = get(result, 'groups[0].buckets', []).map(bucket => ({ key: bucket.key, count: bucket.count }));
    return new Immutable.Map(result)
      .set('id', result.id)
      .set('results', aggregationResults)
      .toJS();
  },
};
