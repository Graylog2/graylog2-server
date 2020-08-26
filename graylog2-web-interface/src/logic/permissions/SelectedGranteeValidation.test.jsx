// @flow strict
import { Map } from 'immutable';

import SelectedGranteeValidation from './SelectedGranteeValidation';

describe('SelectedGranteeValidation', () => {
  it('should return a valid result if there is at least one owner left', () => {
    const selectedGranteeCapabilities = Map({ 'grn::::user:someone-id': 'owner' });
    const result = SelectedGranteeValidation(selectedGranteeCapabilities);

    expect(result.valid).toBeTruthy();
    expect(result.errors).toBeUndefined();
  });

  it('should return a valid result if there multiple owner left', () => {
    const selectedGranteeCapabilities = Map({ 'grn::::user:someone-id': 'owner', 'grn::::user:someone-else': 'owner' });
    const result = SelectedGranteeValidation(selectedGranteeCapabilities);

    expect(result.valid).toBeTruthy();
    expect(result.errors).toBeUndefined();
  });


  it('should return a invalid result if there is at least no owner left', () => {
    const selectedGranteeCapabilities = Map({ 'grn::::user:someone-id': 'manager' });
    const result = SelectedGranteeValidation(selectedGranteeCapabilities);

    expect(result.valid).toBeFalsy();
    expect(result.errors?.owner).toBe('There must be at least one owner');
  });

  it('should return a invalid result if there is at least no capability', () => {
    const selectedGranteeCapabilities = Map();
    const result = SelectedGranteeValidation(selectedGranteeCapabilities);

    expect(result.valid).toBeFalsy();
    expect(result.errors?.owner).toBe('There must be at least one owner');
  });
});
