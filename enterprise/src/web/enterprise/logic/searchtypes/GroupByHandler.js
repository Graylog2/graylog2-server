import Immutable from 'immutable';

export default {
  convert(result) {
    const groupByResults = result.groups.map((group) => {
      const groupResult = {
        count: group.count,
      };
      group.fields.forEach((entry) => { groupResult[entry.field] = entry.value; });
      return groupResult;
    });
    return new Immutable.Map(result)
      .set('results', groupByResults)
      .toJS();
  },
};
