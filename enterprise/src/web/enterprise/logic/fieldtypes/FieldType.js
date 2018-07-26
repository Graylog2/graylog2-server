import Immutable from 'immutable';

const Properties = {
  Compound: 'compound',
  Enumerable: 'enumerable',
  FullTextSearch: 'full-text-search',
  Numeric: 'numeric',
};

class FieldType {
  constructor(type, properties, indexNames) {
    this.value = Immutable.Map({ type, properties: Immutable.Set(properties), indexNames: Immutable.Set(indexNames) });
  }

  static Unknown = new FieldType('unknown', [], []);

  get type() {
    return this.value.get('type');
  }

  get properties() {
    return this.value.get('properties');
  }

  get indexNames() {
    return this.value.get('indexNames');
  }

  isNumeric() {
    return this.properties.has(Properties.Numeric);
  }

  isCompound() {
    return this.properties.has(Properties.Compound);
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { type, properties, index_names } = value;
    return new FieldType(type, properties, index_names);
  }
}

export default FieldType;
