// @flow strict
import type { SelectedGranteeCapabilities } from './EntityShareState';

export type ValidationResult = {
  valid: boolean,
  errors?: {
    [string]: string,
  },
};

const SelectedGranteeValidation = (selectedGranteeCapabilities: SelectedGranteeCapabilities): ValidationResult => {
  const ownerCount = selectedGranteeCapabilities.count((capability) => capability === 'own');

  if (ownerCount >= 1) {
    return { valid: true };
  }

  return { valid: false, errors: { owner: 'There must be at least one owner' } };
};

export default SelectedGranteeValidation;
