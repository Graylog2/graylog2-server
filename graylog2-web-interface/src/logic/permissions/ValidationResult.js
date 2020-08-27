// @flow strict
import * as Immutable from 'immutable';

type Errors = {
  selectedGranteeCapabilities: Immutable.List<string>,
};

type InternalState = {
  errors: Errors,
  failed: boolean,
};

export type ValidationResultJSON = {
  errors: {
    selected_grantee_capabilities: string[],
  },
  failed: boolean,
};

export default class ValidationResult {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(errors: $PropertyType<InternalState, 'errors'>, failed: $PropertyType<InternalState, 'failed'>) {
    this._value = { errors, failed };
  }

  get errors() {
    return this._value.errors;
  }

  get failed() {
    return this._value.failed;
  }

  toBuilder() {
    const {
      errors,
      failed,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      errors,
      failed,
    }));
  }

  // eslint-disable-next-line no-undef
  static create(errors: $PropertyType<InternalState, 'errors'>, failed: $PropertyType<InternalState, 'failed'>) {
    return new ValidationResult(errors, failed);
  }

  toJSON() {
    const { errors, failed } = this._value;

    return {
      errors: {
        selected_grantee_capabilities: errors.selectedGranteeCapabilities,
      },
      failed,
    };
  }

  static fromJSON(value: ValidationResultJSON = {}) {
    const { errors: errorsJson = {}, failed } = value;
    console.log('fromJson', value);
    const errors = {
      selectedGranteeCapabilities: Immutable.List(errorsJson.selected_grantee_capabilities),
    };

    return ValidationResult.create(errors, failed);
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  // eslint-disable-next-line no-undef
  errors(value: $PropertyType<InternalState, 'errors'>) {
    return new Builder(this.value.set('errors', value));
  }

  // eslint-disable-next-line no-undef
  failed(value: $PropertyType<InternalState, 'failed'>) {
    return new Builder(this.value.set('failed', value));
  }

  build() {
    const { errors, failed } = this.value.toObject();

    return new ValidationResult(errors, failed);
  }
}
