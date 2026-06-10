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
type PossibleError = {
  invalid: boolean;
  message: string;
};

type FormFieldContext = {
  value?: string | number | boolean;
  error?: string;
};

type FormContext = {
  [field: string]: FormFieldContext | undefined;
};

const formValidation = {
  checkInputValidity: (input: HTMLInputElement, customErrorMessage?: string): string | undefined => {
    const { validity } = input;
    const isValid: boolean = validity.valid;
    const providedMessage: string | null = input.getAttribute('title');

    if (isValid) {
      return undefined;
    }

    const isEmpty: boolean = validity.valueMissing;
    const isIncorrectType: boolean = validity.typeMismatch;
    const isIncorrectPattern: boolean = validity.patternMismatch;
    const isTooLong: boolean = validity.tooLong || validity.rangeOverflow;
    const isTooShort: boolean = validity.rangeUnderflow;
    const isInvalidStep: boolean = validity.stepMismatch;

    const possibleErrors: Array<PossibleError> = [
      { invalid: isEmpty, message: providedMessage || 'A value is required.' },
      {
        invalid: isIncorrectType,
        message: providedMessage || 'Please verify that your input is the expected type (ie: email address).',
      },
      { invalid: isIncorrectPattern, message: providedMessage || 'This input does not match the expected value.' },
      { invalid: isTooLong, message: providedMessage || 'Your input is too long.' },
      { invalid: isTooShort, message: providedMessage || 'Your input is too short.' },
      { invalid: isInvalidStep, message: providedMessage || 'Unexpected value.' },
    ];

    const errorOutput: PossibleError | undefined = possibleErrors.find((error) => error.invalid);

    return customErrorMessage || errorOutput?.message;
  },

  isFormValid: (requiredFields: Array<string>, context: FormContext): boolean =>
    !!requiredFields.find((field: string) => !context[field] || !context[field]?.value || !!context[field]?.error),
};

export default formValidation;
