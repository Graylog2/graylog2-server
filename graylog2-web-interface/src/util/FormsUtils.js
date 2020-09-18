import NumberUtils from 'util/NumberUtils';

import createEvent from './CreateEvent';

export const validateValue = (fieldValue, validationType, validationValue) => {
  switch (validationType) {
    case 'required':
      if (!fieldValue) return 'Field is required';
      break;
    case 'min':
      if (fieldValue < Number(validationValue)) return `Must be greater than ${validationValue}`;
      break;
    case 'max':
      if (fieldValue > Number(validationValue)) return `Must be smaller than ${validationValue}`;
      break;
    default:
      return undefined;
  }

  return undefined;
};

export const getValueFromInput = (input) => {
  switch (input.type) {
    case 'radio':
      return (input.value === 'true' || input.value === 'false' ? input.value === 'true' : input.value);
    case 'checkbox':
      return input.checked;
    case 'number':
      return (input.value === '' || !NumberUtils.isNumber(input.value) ? undefined : Number(input.value));
    default:
      return input.value;
  }
};

export const triggerInput = (urlInput) => {
  const { input } = urlInput;
  const tracker = input._valueTracker;
  const event = createEvent('change');

  event.simulated = true;

  if (tracker) {
    tracker.setValue('');
  }

  input.dispatchEvent(event);
};

export const validation = {
  isRequired: (field) => (value) => (!value ? `The ${field} is required` : undefined),
  hasErrors: (errorMap = {}) => Object.keys(errorMap).length > 0,
};

export const validateField = (validationRules) => (fieldValue) => {
  let error;

  Object.entries(validationRules).some(([validationType, validationValue]) => {
    const validationResult = validateValue(fieldValue, validationType, validationValue);

    if (validationResult) {
      error = validationResult;

      return true;
    }

    return false;
  });

  return error;
};

export default {
  getValueFromInput,
  triggerInput,
  validation,
  validateField,
};
