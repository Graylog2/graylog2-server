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
import { getStepKeys, normalizeStepKey } from './EventDefinitionForm';

describe('EventDefinitionForm step keys', () => {
  it('uses the "additional-details" key for the renamed step', () => {
    expect(getStepKeys(true)).toContain('additional-details');
    expect(getStepKeys(true)).not.toContain('fields');
  });

  it('maps the legacy "fields" step key to "additional-details"', () => {
    expect(normalizeStepKey('fields')).toBe('additional-details');
  });

  it('leaves current and unknown step keys unchanged', () => {
    expect(normalizeStepKey('additional-details')).toBe('additional-details');
    expect(normalizeStepKey('condition')).toBe('condition');
    expect(normalizeStepKey(undefined)).toBeUndefined();
  });
});
