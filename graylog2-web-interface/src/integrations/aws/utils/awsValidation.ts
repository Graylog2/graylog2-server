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

/**
 * Validates that an External ID is only provided when an Assume Role ARN is also provided.
 * External ID without ARN is meaningless and could confuse users into thinking it's being used.
 *
 * @param assumeRoleArn - The Assume Role ARN value
 * @param externalId - The External ID value
 * @returns An error message if validation fails, undefined if valid
 */
export const validateExternalIdRequiresArn = (assumeRoleArn?: string, externalId?: string): string | undefined => {
  const hasExternalId = externalId && externalId.trim().length > 0;
  const hasAssumeRoleArn = assumeRoleArn && assumeRoleArn.trim().length > 0;

  if (hasExternalId && !hasAssumeRoleArn) {
    return 'External ID can only be used when an Assume Role ARN is provided.';
  }

  return undefined;
};

export default {
  validateExternalIdRequiresArn,
};
