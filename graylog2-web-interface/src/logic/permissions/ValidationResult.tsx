/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as Immutable from 'immutable';
import { $PropertyType } from 'utility-types';

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

/* eslint-disable camelcase */
export type ValidationResultJSON = {
  errors: {
    selected_grantee_capabilities: string[],
  },
  error_context: {
    selected_grantee_capabilities: string[],
  },
  failed: boolean,
};
/* eslint-enable camelcase */

export default class ValidationResult {
  _value: InternalState;

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

    // eslint-disable-next-line no-use-before-define,@typescript-eslint/no-use-before-define
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

  static createSuccess() {
    return ValidationResult.create(
      { selectedGranteeCapabilities: Immutable.List() },
      { selectedGranteeCapabilities: Immutable.List() },
      false,
    );
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

  static fromJSON(value: ValidationResultJSON | undefined | null) {
    if (!value) {
      return ValidationResult.createSuccess();
    }

    // eslint-disable-next-line camelcase
    const { errors: errorsJson, error_context, failed } = value;
    const errors = {
      selectedGranteeCapabilities: Immutable.List(errorsJson.selected_grantee_capabilities),
    };

    const errorContext = {
      selectedGranteeCapabilities: Immutable.List(error_context.selected_grantee_capabilities),
    };

    return ValidationResult.create(errors, errorContext, failed);
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define,@typescript-eslint/no-use-before-define
    return new Builder();
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  errors(value: $PropertyType<InternalState, 'errors'>) {
    return new Builder(this.value.set('errors', value));
  }

  errorContext(value: $PropertyType<InternalState, 'errorContext'>) {
    return new Builder(this.value.set('errorContext', value));
  }

  failed(value: $PropertyType<InternalState, 'failed'>) {
    return new Builder(this.value.set('failed', value));
  }

  build() {
    const { errors, errorContext, failed } = this.value.toObject();

    return new ValidationResult(errors, errorContext, failed);
  }
}
