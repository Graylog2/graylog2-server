// @flow strict
import { List, Map, Set } from 'immutable';
import { filtersToStreamSet } from './Query';

describe('Query', () => {
  describe('filtersToStreamSet', () => {
    const singleFilter = Map({
      type: 'stream',
      filters: null,
      id: '000000000000000000000001',
      title: null,
    });

    const filter = Map({
      type: 'or',
      filters: List([
        Map({
          type: 'stream',
          filters: null,
          id: '000000000000000000000001',
          title: null,
        }),
        Map({
          type: 'stream',
          filters: null,
          id: '5c2e07eeba33a9681ad6070a',
          title: null,
        }),
        Map({
          type: 'stream',
          filters: null,
          id: '5d2d9649e117dc4df84cf83c',
          title: null,
        }),
      ]),
    });

    it('returns empty set of stream ids from empty filter', () => {
      expect(filtersToStreamSet(null)).toEqual(Set());
    });
    it('returns set of stream ids from simple filter', () => {
      expect(filtersToStreamSet(singleFilter)).toEqual(Set([
        '000000000000000000000001',
      ]));
    });
    it('returns set of stream ids from two-level filter', () => {
      expect(filtersToStreamSet(filter)).toEqual(Set([
        '000000000000000000000001',
        '5c2e07eeba33a9681ad6070a',
        '5d2d9649e117dc4df84cf83c',
      ]));
    });
  });
});
