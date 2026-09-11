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
import detectPiiSuspect from './piiHeuristics';

describe('detectPiiSuspect', () => {
  it.each([
    ['kay.roepke@example.com', 'email'],
    ['192.168.1.42', 'ip-address'],
    ['2001:db8::8a2e:370:7334', 'ip-address'],
    ['0198c7c2-2c3e-7b90-8f6e-1a2b3c4d5e6f', 'uuid'],
    ['5f3e4c5d6a7b8c9d0e1f2a3b', 'object-id'],
    ['web-prod-01.internal.example.com', 'hostname'],
    ['this is a rather long free text value that nobody should ever put into telemetry', 'free-text'],
  ])('flags %s as %s', (value, rule) => {
    expect(detectPiiSuspect(value)).toBe(rule);
  });

  it.each([
    ['fleet-1'],
    ['onboarding-generate'],
    ['P30D'],
    ['never'],
    ['online-receiving'],
    ['system/collectors/deployment'],
    ['collectors-overview'],
    ['short text'],
  ])('does not flag %s', (value) => {
    expect(detectPiiSuspect(value)).toBeNull();
  });

  it.each([[42], [true], [null], [undefined], [{ nested: true }]])('ignores non-string value %p', (value) => {
    expect(detectPiiSuspect(value)).toBeNull();
  });
});
