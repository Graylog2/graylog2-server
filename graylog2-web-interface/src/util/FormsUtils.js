import NumberUtils from 'util/NumberUtils';

import createEvent from './CreateEvent';

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

export const formHasErrors = (errorMap = {}) => Object.keys(errorMap).length > 0;

export const validateValue = (fieldValue, conditionType, conditionValue) => {
  switch (conditionType) {
    case 'required':
      if (!fieldValue || (fieldValue?.size === 0)) return 'Field is required.';
      break;
    case 'min':
      if (fieldValue < Number(conditionValue)) return `Must be greater than ${conditionValue}.`;
      break;
    case 'max':
      if (fieldValue > Number(conditionValue)) return `Must be smaller than ${conditionValue}.`;
      break;
    default:
      return undefined;
  }

  return undefined;
};

export const validateField = (validationRules = {}) => (fieldValue) => {
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
  formHasErrors,
  validateField,
};
