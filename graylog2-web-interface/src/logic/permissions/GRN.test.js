// @flow strict
import { createGRN, getValuesFromGRN } from './GRN';

describe('GRN', () => {
  it('createGRN should generate GRN with correct format', () => {
    expect(createGRN('dashboard', 'dashboard-id')).toBe('grn::::dashboard:dashboard-id');
  });

  it('getValuesFromGRN should extract correct values from GRN', () => {
    expect(getValuesFromGRN('grn::::stream:stream-id')).toStrictEqual({
      resourceNameType: 'grn',
      cluster: undefined,
      tenent: undefined,
      scope: undefined,
      type: 'stream',
      id: 'stream-id',
    });
  });
});
