import ObjectUtils from 'util/ObjectUtils';

describe('ObjectUtils', () => {
  describe('ObjectUtils#clone()', () => {
    it('should clone the given object', () => {
      const a = { hello: 'world' };
      const b = ObjectUtils.clone(a);

      expect(a.hello).toEqual('world');
      expect(b.hello).toEqual('world');

      b.hello = 'nope';

      expect(a.hello).toEqual('world');
      expect(b.hello).toEqual('nope');
    });
  });

  describe('ObjectUtils#isEmpty()', () => {
    it('should return true for empty objects', () => {
      expect(ObjectUtils.isEmpty({})).toEqual(true);
    });

    it('should return false for non-empty objects', () => {
      expect(ObjectUtils.isEmpty({ hello: 'world' })).toEqual(false);
    });
  });
});
