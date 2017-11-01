import URI from 'urijs';

let Routes;
const prefix = '/test';

describe('Routes', () => {
  describe('without prefix', () => {
    beforeAll(() => {
      jest.resetModules();
      window.appConfig = {}; // Ensure no prefix is set
      Routes = require.requireActual('./Routes');
    });

    it('returns a route from constant', () => {
      expect(Routes.SEARCH).toMatch('/search');
    });

    it('returns a route from function', () => {
      expect(Routes.node('id')).toMatch('/system/nodes/id');
    });

    it('routes contain query parameters', () => {
      const uri = URI(Routes.search('', { rangetype: 'relative', relative: 300 }, 'hour'));
      expect(uri.path()).toMatch('/search');
      expect(uri.hasQuery('q', '')).toBeTruthy();
      expect(uri.hasQuery('rangetype', 'relative')).toBeTruthy();
      expect(uri.hasQuery('relative', '300')).toBeTruthy();
      expect(uri.hasQuery('interval', 'hour')).toBeTruthy();
    });
  });

  describe('with prefix', () => {
    beforeAll(() => {
      jest.resetModules();
      window.appConfig = {
        gl2AppPathPrefix: prefix,
      };
      Routes = require.requireActual('./Routes');
    });

    it('returns a route from constant', () => {
      expect(Routes.SEARCH).toMatch(`${prefix}/search`);
    });

    it('returns a route from function', () => {
      expect(Routes.node('id')).toMatch(`${prefix}/system/nodes/id`);
    });

    it('routes contain query parameters', () => {
      const uri = URI(Routes.search('', { rangetype: 'relative', relative: 300 }, 'hour'));
      expect(uri.path()).toMatch(`${prefix}/search`);
      expect(uri.hasQuery('q', '')).toBeTruthy();
      expect(uri.hasQuery('rangetype', 'relative')).toBeTruthy();
      expect(uri.hasQuery('relative', '300')).toBeTruthy();
      expect(uri.hasQuery('interval', 'hour')).toBeTruthy();
    });
  });
});
