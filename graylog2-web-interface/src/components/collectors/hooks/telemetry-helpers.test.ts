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
import { classifyHostname, classifyInputBind } from './telemetry-helpers';

describe('classifyHostname', () => {
  it.each([
    ['10.0.0.5', 'ip'],
    ['192.168.1.1', 'ip'],
    ['127.0.0.1', 'ip'],
    ['::1', 'ip'],
    ['2001:db8::1', 'ip'],
    ['graylog.example.com', 'hostname'],
    ['localhost', 'hostname'],
    ['my-server', 'hostname'],
    ['', 'hostname'],
  ])('classifies %s as %s', (input, expected) => {
    expect(classifyHostname(input)).toBe(expected);
  });
});

describe('classifyInputBind', () => {
  it.each([
    ['', 'wildcard'],
    ['0.0.0.0', 'wildcard'],
    ['::', 'wildcard'],
    ['*', 'wildcard'],
    ['10.0.0.5', 'specific'],
    ['eth0.local', 'specific'],
    [undefined, 'unknown'],
    [null, 'unknown'],
  ])('classifies %p as %s', (input, expected) => {
    expect(classifyInputBind(input as string | undefined)).toBe(expected);
  });
});
