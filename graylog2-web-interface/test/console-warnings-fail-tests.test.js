/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/* eslint-disable no-console */

describe('console-warnings-fail-tests', () => {
  /*
  We are suppressing console output for the following tests here. We are not reusing `suppressConsole`, as it does not
  work well with the order of wrapping original `console` and importing/requiring the actual SUT module, also it does
  not currently suppress `console.warn`.
   */
  let oldConsoleWarn;
  let oldConsoleError;
  beforeAll(() => {
    oldConsoleWarn = console.origWarn;
    oldConsoleError = console.origError;
    console.origWarn = () => {};
    console.origError = () => {};
  });

  afterAll(() => {
    console.origWarn = oldConsoleWarn;
    console.origError = oldConsoleError;
  });
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
