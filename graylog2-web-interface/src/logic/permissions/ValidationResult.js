// @flow strict
import * as Immutable from 'immutable';

import type { GRN } from './types';

type Errors = {
  selectedGranteeCapabilities: Immutable.List<string>,
};

type ErrorContext = {
  selectedGranteeCapabilities: Immutable.List<GRN>,
};

type InternalState = {
  errors: Errors,
  errorContext: ErrorContext,
  failed: boolean,
};

export type ValidationResultJSON = {
  errors: {
    selected_grantee_capabilities: string[],
  },
  error_context: {
    selected_grantee_capabilities: string[],
  },
  failed: boolean,
};

export default class ValidationResult {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(
    errors: $PropertyType<InternalState, 'errors'>,
    errorContext: $PropertyType<InternalState, 'errorContext'>,
    failed: $PropertyType<InternalState, 'failed'>,
  ) {
    this._value = {
      errors,
      errorContext,
      failed,
    };
  }

  get errors() {
    return this._value.errors;
  }

  get errorContext() {
    return this._value.errorContext;
  }

  get failed() {
    return this._value.failed;
  }

  toBuilder() {
    const {
      errors,
      errorContext,
      failed,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      errors,
      errorContext,
      failed,
    }));
  }

  // eslint-disable-next-line no-undef
  static create(
    errors: $PropertyType<InternalState, 'errors'>,
    errorContext: $PropertyType<InternalState, 'errorContext'>,
    failed: $PropertyType<InternalState, 'failed'>,
  ) {
    return new ValidationResult(errors, errorContext, failed);
  }

  toJSON() {
    const { errors, errorContext, failed } = this._value;

    return {
      errors: {
        selected_grantee_capabilities: errors.selectedGranteeCapabilities,
      },
      error_context: {
        selected_grantee_capabilities: errorContext.selectedGranteeCapabilities,
      },
      failed,
    };
  }

  static fromJSON(value: ValidationResultJSON = {}) {
    // eslint-disable-next-line camelcase
    const { errors: errorsJson = {}, error_context = {}, failed } = value;
    const errors = {
      selectedGranteeCapabilities: Immutable.List(errorsJson.selected_grantee_capabilities),
    };

    const errorContext = {
      selectedGranteeCapabilities: Immutable.List(error_context.selected_grantee_capabilities),
    };

    return ValidationResult.create(errors, errorContext, failed);
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
  errorContext(value: $PropertyType<InternalState, 'errorContext'>) {
    return new Builder(this.value.set('errorContext', value));
  }

  // eslint-disable-next-line no-undef
  failed(value: $PropertyType<InternalState, 'failed'>) {
    return new Builder(this.value.set('failed', value));
  }

  build() {
    const { errors, errorContext, failed } = this.value.toObject();

    return new ValidationResult(errors, errorContext, failed);
  }
}
