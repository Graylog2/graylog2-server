import { Map } from 'immutable';
import Constraint from './Constraint';

export default class Entity {
  constructor(v, type, id, data, constraintValues = []) {
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
    };
  }

  static fromJSON(value) {
    const { v, type, id, data, constraints } = value;
    return new Entity(v, type, id, data, constraints);
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

  get constraints() {
    return this._value.constraints;
  }

  get title() {
    const { data } = this._value;
    return (data.title || data.name || {}).value || '';
  }

  get description() {
    const { data } = this._value;
    return (data.description || {}).value || '';
  }

  toBuilder() {
    const {
      v,
      type,
      id,
      data,
      constraints,
    } = this._value;
    /* eslint-disable-next-line no-use-before-define */
    return new Builder(Map({
      v,
      type,
      id,
      data,
      constraints,
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
    } = this.value.toObject();
    return new Entity(v, type, id, data, constraints);
  }
}
