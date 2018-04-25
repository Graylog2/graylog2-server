export default class Pivot {
  constructor(field, type, config = {}) {
    this._value = { field, type, config };
  }
  get field() {
    return this._value.field;
  }

  get type() {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  static fromJSON(value) {
    const { field, type, config = {} } = value;
    return new Pivot(field, type, config);
  }

  toJSON() {
    const { field, type, config } = this._value;
    return { field, type, config };
  }
}
