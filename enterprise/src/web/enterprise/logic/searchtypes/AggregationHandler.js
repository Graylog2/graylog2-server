import Immutable from 'immutable';

export default {
  convert(result) {
    const aggregationResults = result.groups[0].buckets.map(bucket => ({ key: bucket.key, count: bucket.count }));
    return new Immutable.Map(result)
      .set('id', result.id)
      .set('results', aggregationResults)
      .toJS();
  },
};
