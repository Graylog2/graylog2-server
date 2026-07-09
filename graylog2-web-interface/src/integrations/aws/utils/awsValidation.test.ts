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

import { validateExternalIdRequiresArn } from './awsValidation';

describe('awsValidation', () => {
  describe('validateExternalIdRequiresArn', () => {
    it('returns undefined when both ARN and External ID are provided', () => {
      const result = validateExternalIdRequiresArn(
        'arn:aws:iam::123456789012:role/test-role',
        'test-external-id',
      );
      expect(result).toBeUndefined();
    });

    it('returns undefined when neither ARN nor External ID are provided', () => {
      const result = validateExternalIdRequiresArn(undefined, undefined);
      expect(result).toBeUndefined();
    });

    it('returns undefined when only ARN is provided', () => {
      const result = validateExternalIdRequiresArn('arn:aws:iam::123456789012:role/test-role', undefined);
      expect(result).toBeUndefined();
    });

    it('returns undefined when ARN is provided and External ID is empty string', () => {
      const result = validateExternalIdRequiresArn('arn:aws:iam::123456789012:role/test-role', '');
      expect(result).toBeUndefined();
    });

    it('returns error message when External ID is provided without ARN', () => {
      const result = validateExternalIdRequiresArn(undefined, 'test-external-id');
      expect(result).toBe('External ID can only be used when an Assume Role ARN is provided.');
    });

    it('returns error message when External ID is provided with empty ARN', () => {
      const result = validateExternalIdRequiresArn('', 'test-external-id');
      expect(result).toBe('External ID can only be used when an Assume Role ARN is provided.');
    });

    it('returns error message when External ID is provided with whitespace-only ARN', () => {
      const result = validateExternalIdRequiresArn('   ', 'test-external-id');
      expect(result).toBe('External ID can only be used when an Assume Role ARN is provided.');
    });

    it('returns error message when External ID is whitespace and ARN is provided', () => {
      const result = validateExternalIdRequiresArn('arn:aws:iam::123456789012:role/test-role', '   ');
      expect(result).toBeUndefined();
    });
  });
});
