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
import useableCssProxy from 'css/useable-css-proxy';

describe('useable-css-proxy', () => {
  it('should return a proxy object', () => {
    expect(useableCssProxy).toBeDefined();
  });
  it('should return a proxy object for a key', () => {
    const result = useableCssProxy.container;
    expect(result).toBeDefined();
    expect(result.toString()).toEqual('container');
  });
});
