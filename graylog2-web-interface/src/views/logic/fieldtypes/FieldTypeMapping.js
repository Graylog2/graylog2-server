// @flow strict
import FieldType from './FieldType';
import type { FieldTypeJSON } from './FieldType';

export type FieldTypeMappingJSON = {
  name: string,
  type: FieldTypeJSON,
};

class FieldTypeMapping {
  value: {
    name: string,
    type: FieldType,
  };

  constructor(name: string, type: FieldType) {
    this.value = { name, type };
  }

  get name() {
    return this.value.name;
  }

  get type() {
    return this.value.type;
  }

  static fromJSON(value: FieldTypeMappingJSON) {
    const { name, type } = value;
    return new FieldTypeMapping(name, FieldType.fromJSON(type));
  }

  static create(name: string, type: FieldType) {
    return new FieldTypeMapping(name, type);
  }
}

export default FieldTypeMapping;
