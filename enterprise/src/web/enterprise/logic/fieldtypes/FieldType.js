// @flow strict
import * as Immutable from 'immutable';

const Properties = {
  Compound: 'compound',
  Enumerable: 'enumerable',
  FullTextSearch: 'full-text-search',
  Numeric: 'numeric',
};

export type FieldTypeJSON = {
  type: string,
  properties: Array<string>,
  index_names: Array<string>
};

class FieldType {
  value: Immutable.Map<string, *>;
  constructor(type: string, properties: Array<string>, indexNames: Array<string>) {
    this.value = Immutable.Map({ type, properties: Immutable.Set(properties), indexNames: Immutable.Set(indexNames) });
  }

  static Unknown = new FieldType('unknown', [], []);

  get type(): string {
    return this.value.get('type');
  }

  get properties(): Immutable.Set<string> {
    return this.value.get('properties');
  }

  get indexNames(): Immutable.Set<string> {
    return this.value.get('indexNames');
  }

  isNumeric(): boolean {
    return this.properties.has(Properties.Numeric);
  }

  isCompound(): boolean {
    return this.properties.has(Properties.Compound);
  }

  static fromJSON(value: FieldTypeJSON) {
    // eslint-disable-next-line camelcase
    const { type, properties, index_names } = value;
    return new FieldType(type, properties, index_names);
  }
}

export default FieldType;
