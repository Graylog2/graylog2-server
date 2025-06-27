import type { FormDataType } from './types';

const formValidation = {
  checkInputValidity: (input: HTMLInputElement, customErrorMessage: string) => {
    const { validity } = input;
    const isValid = validity.valid;
    const providedMessage = input.getAttribute('title');

    if (isValid) {
      return undefined;
    }

    const isEmpty = validity.valueMissing;
    const isIncorrectType = validity.typeMismatch;
    const isIncorrectPattern = validity.patternMismatch;
    const isTooLong = validity.tooLong || validity.rangeOverflow;
    const isTooShort = validity.rangeUnderflow;
    const isInvalidStep = validity.stepMismatch;

    const possibleErrors = [
      { invalid: isEmpty, message: providedMessage || 'A value is required.' },
      { invalid: isIncorrectType, message: providedMessage || 'Please verify that your input is the expected type.' },
      { invalid: isIncorrectPattern, message: providedMessage || 'This input does not match the expected value.' },
      { invalid: isTooLong, message: providedMessage || 'Your input is too long.' },
      { invalid: isTooShort, message: providedMessage || 'Your input is too short.' },
      { invalid: isInvalidStep, message: providedMessage || 'Unexpected value.' },
    ];

    const errorOutput = possibleErrors.find((error) => error.invalid);

    return customErrorMessage || errorOutput.message;
  },

  isFormValid: (requiredFields: string[], context: FormDataType) =>
    !!requiredFields.find((field) => !context[field] || !context[field].value || context[field].error),
};

export default formValidation;
