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
import PermissionsMixin from './PermissionsMixin';
const { isPermitted, isAnyPermitted } = PermissionsMixin;

declare module 'graylog-web-plugin/plugin' {
  interface EntityActions {
    foo: 'read';
    bar: 'read';
    baz: 'read';
  }
}

describe('PermissionsMixin', () => {
  describe('isPermitted', () => {
    it('returns `true` when required permissions are `undefined`', () => {
      expect(isPermitted([], undefined)).toBeTruthy();
    });

    it('returns `true` when required permissions are empty list', () => {
      expect(isPermitted([], [])).toBeTruthy();
    });

    it('returns `false` when possessed permissions are `undefined` and required permissions are empty', () => {
      expect(isPermitted(undefined, ['foo:read'])).toBeFalsy();
    });

    it('returns `false` when possessed permissions are `undefined`', () => {
      expect(isPermitted(undefined, ['foo:read'])).toBeFalsy();
    });

    it('returns `true` when wildcard permission is possessed', () => {
      expect(isPermitted(['*'], undefined)).toBeTruthy();
      expect(isPermitted(['*'], [])).toBeTruthy();
      expect(isPermitted(['*'], ['foo:read'])).toBeTruthy();
      expect(isPermitted(['*'], ['foo:read', 'bar:read'])).toBeTruthy();
    });

    it('returns `true` when possessed are identical to required permissions', () => {
      expect(isPermitted(['foo:read'], ['foo:read'])).toBeTruthy();
      expect(isPermitted(['foo:read', 'bar:read'], ['foo:read', 'bar:read'])).toBeTruthy();
      expect(isPermitted(['bar:read', 'foo:read'], ['foo:read', 'bar:read'])).toBeTruthy();
    });

    it('returns `true` when possessed contain all required permissions', () => {
      expect(isPermitted(['foo:read', 'bar:read'], ['foo:read'])).toBeTruthy();
      expect(isPermitted(['foo:read', 'bar:read', 'baz:read'], ['foo:read', 'bar:read'])).toBeTruthy();
      expect(isPermitted(['bar:read', 'baz:read', 'foo:read'], ['foo:read', 'bar:read'])).toBeTruthy();
    });

    it('returns `false` when possessed do not contain all required permissions', () => {
      expect(isPermitted(['foo:read'], ['foo:read', 'bar:read'])).toBeFalsy();
      expect(isPermitted(['foo:read', 'bar:read'], ['foo:read', 'bar:read', 'baz:read'])).toBeFalsy();
      expect(isPermitted(['bar:read', 'foo:read'], ['foo:read', 'bar:read', 'baz:read'])).toBeFalsy();
    });
  });

  describe('isAnyPermitted', () => {
    it('returns `true` when required permissions are `undefined`', () => {
      expect(isAnyPermitted([], undefined)).toBeTruthy();
    });

    it('returns `true` when required permissions are empty list', () => {
      expect(isAnyPermitted([], [])).toBeTruthy();
    });

    it('returns `false` when possessed permissions are `undefined` and required permissions are empty', () => {
      expect(isAnyPermitted(undefined, ['foo:read'])).toBeFalsy();
    });

    it('returns `false` when possessed permissions are `undefined`', () => {
      expect(isAnyPermitted(undefined, ['foo:read'])).toBeFalsy();
    });

    it('returns `true` when wildcard permission is possessed', () => {
      expect(isAnyPermitted(['*'], undefined)).toBeTruthy();
      expect(isAnyPermitted(['*'], [])).toBeTruthy();
      expect(isAnyPermitted(['*'], ['foo:read'])).toBeTruthy();
      expect(isAnyPermitted(['*'], ['foo:read', 'bar:read'])).toBeTruthy();
    });

    it('returns `true` when possessed are identical to required permissions', () => {
      expect(isAnyPermitted(['foo:read'], ['foo:read'])).toBeTruthy();
      expect(isAnyPermitted(['foo:read', 'bar:read'], ['foo:read', 'bar:read'])).toBeTruthy();
      expect(isAnyPermitted(['bar:read', 'foo:read'], ['foo:read', 'bar:read'])).toBeTruthy();
    });

    it('returns `true` when possessed contain all required permissions', () => {
      expect(isAnyPermitted(['foo:read', 'bar:read'], ['foo:read'])).toBeTruthy();
      expect(isAnyPermitted(['foo:read', 'bar:read', 'baz:read'], ['foo:read', 'bar:read'])).toBeTruthy();
      expect(isAnyPermitted(['bar:read', 'baz:read', 'foo:read'], ['foo:read', 'bar:read'])).toBeTruthy();
    });

    it('returns `false` when possessed do not contain all required permissions', () => {
      expect(isAnyPermitted(['foo:read'], ['foo:read', 'bar:read'])).toBeTruthy();
      expect(isAnyPermitted(['foo:read', 'bar:read'], ['foo:read', 'bar:read', 'baz:read'])).toBeTruthy();
      expect(isAnyPermitted(['bar:read', 'foo:read'], ['foo:read', 'bar:read', 'baz:read'])).toBeTruthy();
    });
  });
});
