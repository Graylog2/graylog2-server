// @flow strict
/* eslint-disable no-console */
// eslint-disable-next-line no-unused-vars, import/default
import unused from './console-warnings-fail-tests';

describe('console-warnings-fail-tests', () => {
  describe('console.error', () => {
    it('throws error if used', () => {
      expect(() => { console.error('hello there!'); }).toThrowError(new Error('hello there!'));
    });
    it('does not throw error if containing react deprecation notice', () => {
      expect(() => {
        console.error('Warning: componentWillReceiveProps has been renamed, and is not recommended for use. See https://fb.me/react-unsafe-component-lifecycles for details.');
      }).not.toThrow();
    });
    it('does not throw error if called without arguments', () => {
      expect(() => { console.error(); }).not.toThrow();
    });
  });
  describe('console.warn', () => {
    it('throws error if used', () => {
      expect(() => { console.warn('hello there!'); }).toThrowError(new Error('hello there!'));
    });
    it('does not throw error if containing react deprecation notice', () => {
      expect(() => {
        console.warn('Warning: componentWillReceiveProps has been renamed, and is not recommended for use. See https://fb.me/react-unsafe-component-lifecycles for details.');
      }).not.toThrow();
    });
    it('does not throw error if called without arguments', () => {
      expect(() => { console.warn(); }).not.toThrow();
    });
  });
});
