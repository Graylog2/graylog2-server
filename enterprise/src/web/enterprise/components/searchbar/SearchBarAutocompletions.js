import Immutable from 'immutable';
import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import { ViewMetadataStore } from '../../stores/ViewMetadataStore';

const _fieldResult = (field, score = 1) => {
  const { name, type } = field;
  return {
    name: name,
    value: name,
    score,
    meta: type.type,
  };
};

const _matchesFieldName = (prefix) => {
  return field => (field.name.indexOf(prefix) >= 0);
};

export default class SearchBarAutoCompletions {
  constructor() {
    this.state = FieldTypesStore.getInitialState();
    FieldTypesStore.listen((newState) => {
      this.state = newState;
    });

    this.onViewMetadataStoreUpdate(ViewMetadataStore.getInitialState());
    ViewMetadataStore.listen(this.onViewMetadataStoreUpdate);
  }

  onViewMetadataStoreUpdate = (newState) => {
    const { activeQuery } = newState;
    this.activeQuery = activeQuery;
  };

  getCompletions = (editor, session, pos, prefix, callback) => {
    const { all, queryFields } = this.state;
    const matchesFieldName = _matchesFieldName(prefix);
    const currentQueryFields = queryFields.get(this.activeQuery, Immutable.Set());
    const results = Immutable.List([
      currentQueryFields.filter(matchesFieldName)
        .map(field => _fieldResult(field, 2)),
      all.filter(matchesFieldName)
        .map(field => _fieldResult(field, 1)),
    ]).flatten().toJS();
    callback(null, results);
  }
}
