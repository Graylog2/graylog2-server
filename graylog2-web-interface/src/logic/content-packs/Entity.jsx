import { Map } from 'immutable';
import ValueRefHelper from 'util/ValueRefHelper';
import Constraint from './Constraint';

export default class Entity {
  constructor(v, type, id, data, fromServer = false, constraintValues = []) {
    const constraints = constraintValues.map((c) => {
      if (c instanceof Constraint) {
        return c;
      }
      return Constraint.fromJSON(c);
    });

    this._value = {
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
    };
  }

  static fromJSON(value, fromServer = true) {
    const { v, type, id, data, constraints } = value;
    return new Entity(v, type, id, data, fromServer, constraints);
  }

  get v() {
    return this._value.v;
  }

  get type() {
    return this._value.type;
  }

  get id() {
    return this._value.id;
  }

  get data() {
    return this._value.data;
  }

  get fromServer() {
    return this._value.fromServer;
  }

  get constraints() {
    return this._value.constraints;
  }

  get title() {
    let value = this.getValueFromData('title');
    if (!value) {
      value = this.getValueFromData('name');
    }
    return value || '';
  }

  get description() {
    return this.getValueFromData('description') || '';
  }

  getValueFromData(key) {
    const { data } = this._value;
    if (!data || !data[key]) {
      return undefined;
    }

    if (ValueRefHelper.dataIsValueRef(data[key])) {
      return (data[key] || {})[ValueRefHelper.VALUE_REF_VALUE_FIELD];
    }
    return data[key];
  }

  toBuilder() {
    const {
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
    } = this._value;
    /* eslint-disable-next-line no-use-before-define */
    return new Builder(Map({
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
    }));
  }


  static builder() {
    /* eslint-disable-next-line no-use-before-define */
    return new Builder();
  }

  toJSON() {
    const {
      v,
      type,
      id,
      data,
      constraints,
    } = this._value;
    return {
      v,
      type,
      id,
      data,
      constraints,
    };
  }
}

class Builder {
  constructor(value = Map()) {
    this.value = value;
  }

  v(value) {
    this.value = this.value.set('v', value);
    return this;
  }

  type(value) {
    this.value = this.value.set('type', value);
    return this;
  }

  id(value) {
    this.value = this.value.set('id', value);
    return this;
  }

  data(value) {
    this.value = this.value.set('data', value);
    return this;
  }

  fromServer(value) {
    this.value = this.value.set('fromServer', value);
    return this;
  }

  constraints(value) {
    this.value = this.value.set('constraints', value);
    return this;
  }

  build() {
    const {
      v,
      type,
      id,
      data,
      constraints,
      fromServer,
    } = this.value.toObject();
    return new Entity(v, type, id, data, fromServer, constraints);
  }
}
