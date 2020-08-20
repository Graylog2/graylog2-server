// @flow strict
import Routes from 'routing/Routes';

import { createGRN, getValuesFromGRN, getShowRouteFromGRN } from './GRN';

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

  describe('getShowRouteFromGRN should return correct route for', () => {
    const createsCorrectShowEntityURL = ({ grn, entityShowURL }) => {
      expect(getShowRouteFromGRN(grn)).toBe(entityShowURL);
    };

    it.each`
      type            | grn                                 | entityShowURL
      ${'user'}       | ${'grn::::user:user-id'}            | ${Routes.SYSTEM.USERS.show('user-id')}
      ${'stream'}     | ${'grn::::stream:stream-id'}        | ${Routes.stream_search('stream-id')}
    `('type $type with grn $grn', createsCorrectShowEntityURL);
  });
});
