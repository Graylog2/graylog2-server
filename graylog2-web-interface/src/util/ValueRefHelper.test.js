import ValueRefHelper from 'util/ValueRefHelper';

describe('ValueReferenceData', () => {
  describe('ValueRefHelper#dataIsValueRef', () => {
    it('returns false for a "null" value', () => {
      expect(ValueRefHelper.dataIsValueRef(null)).toEqual(false);
    });
  });
});
