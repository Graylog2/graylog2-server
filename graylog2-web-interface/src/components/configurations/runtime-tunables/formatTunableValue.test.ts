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
import formatTunableValue from './formatTunableValue';

describe('formatTunableValue', () => {
  it('formats enum values with their labels', () => {
    const schema = { type: ['string', 'null'], enum: ['s3', 'gcs', null], 'x-graylog-enum-labels': { gcs: 'Google Cloud Storage' } };

    expect(formatTunableValue(schema, 'gcs')).toBe('Google Cloud Storage');
    expect(formatTunableValue(schema, 's3')).toBe('s3');
    expect(formatTunableValue(schema, null)).toBe('Not set');
  });
});
