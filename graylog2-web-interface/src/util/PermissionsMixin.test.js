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

describe('PermissionsMixin', () => {
  describe('isPermitted', () => {
    it('returns `true` when required permissions are `undefined`', () => {
      expect(isPermitted([], undefined)).toBeTruthy();
    });

    it('returns `true` when required permissions are empty list', () => {
      expect(isPermitted([], [])).toBeTruthy();
    });

    it('returns `false` when possessed permissions are `undefined` and required permissions are empty', () => {
      expect(isPermitted(undefined, ['foo'])).toBeFalsy();
    });

    it('returns `false` when possessed permissions are `undefined`', () => {
      expect(isPermitted(undefined, ['foo'])).toBeFalsy();
    });

    it('returns `true` when wildcard permission is possessed', () => {
      expect(isPermitted(['*'], undefined)).toBeTruthy();
      expect(isPermitted(['*'], [])).toBeTruthy();
      expect(isPermitted(['*'], ['foo'])).toBeTruthy();
      expect(isPermitted(['*'], ['foo', 'bar'])).toBeTruthy();
    });

    it('returns `true` when possessed are identical to required permissions', () => {
      expect(isPermitted(['foo'], ['foo'])).toBeTruthy();
      expect(isPermitted(['foo', 'bar'], ['foo', 'bar'])).toBeTruthy();
      expect(isPermitted(['bar', 'foo'], ['foo', 'bar'])).toBeTruthy();
    });

    it('returns `true` when possessed contain all required permissions', () => {
      expect(isPermitted(['foo', 'bar'], ['foo'])).toBeTruthy();
      expect(isPermitted(['foo', 'bar', 'baz'], ['foo', 'bar'])).toBeTruthy();
      expect(isPermitted(['bar', 'baz', 'foo'], ['foo', 'bar'])).toBeTruthy();
    });

    it('returns `false` when possessed do not contain all required permissions', () => {
      expect(isPermitted(['foo'], ['foo', 'bar'])).toBeFalsy();
      expect(isPermitted(['foo', 'bar'], ['foo', 'bar', 'baz'])).toBeFalsy();
      expect(isPermitted(['bar', 'foo'], ['foo', 'bar', 'baz'])).toBeFalsy();
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
      expect(isAnyPermitted(undefined, ['foo'])).toBeFalsy();
    });

    it('returns `false` when possessed permissions are `undefined`', () => {
      expect(isAnyPermitted(undefined, ['foo'])).toBeFalsy();
    });

    it('returns `true` when wildcard permission is possessed', () => {
      expect(isAnyPermitted(['*'], undefined)).toBeTruthy();
      expect(isAnyPermitted(['*'], [])).toBeTruthy();
      expect(isAnyPermitted(['*'], ['foo'])).toBeTruthy();
      expect(isAnyPermitted(['*'], ['foo', 'bar'])).toBeTruthy();
    });

    it('returns `true` when possessed are identical to required permissions', () => {
      expect(isAnyPermitted(['foo'], ['foo'])).toBeTruthy();
      expect(isAnyPermitted(['foo', 'bar'], ['foo', 'bar'])).toBeTruthy();
      expect(isAnyPermitted(['bar', 'foo'], ['foo', 'bar'])).toBeTruthy();
    });

    it('returns `true` when possessed contain all required permissions', () => {
      expect(isAnyPermitted(['foo', 'bar'], ['foo'])).toBeTruthy();
      expect(isAnyPermitted(['foo', 'bar', 'baz'], ['foo', 'bar'])).toBeTruthy();
      expect(isAnyPermitted(['bar', 'baz', 'foo'], ['foo', 'bar'])).toBeTruthy();
    });

    it('returns `false` when possessed do not contain all required permissions', () => {
      expect(isAnyPermitted(['foo'], ['foo', 'bar'])).toBeTruthy();
      expect(isAnyPermitted(['foo', 'bar'], ['foo', 'bar', 'baz'])).toBeTruthy();
      expect(isAnyPermitted(['bar', 'foo'], ['foo', 'bar', 'baz'])).toBeTruthy();
    });
  });
});
