import FieldType from './FieldType';

class FieldTypeMapping {
  constructor(name, type) {
    this.value = { name, type };
  }

  get name() {
    return this.value.name;
  }

  get type() {
    return this.value.type;
  }

  static fromJSON(value) {
    const { name, type } = value;
    return new FieldTypeMapping(name, FieldType.fromJSON(type));
  }
}

export default FieldTypeMapping;
