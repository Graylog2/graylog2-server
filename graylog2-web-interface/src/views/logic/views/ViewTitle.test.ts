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
import View from 'views/logic/views/View';

import ViewTitle from './ViewTitle';

describe('ViewTitle', () => {
  it('returns default title for unsaved search', () => {
    expect(ViewTitle(undefined, View.Type.Search)).toBe('Unsaved Search');
  });

  it('returns default title for unsaved dashboard', () => {
    expect(ViewTitle(undefined, View.Type.Dashboard)).toBe('Unsaved Dashboard');
  });

  it('returns actual title if present', () => {
    expect(ViewTitle('My title', undefined)).toBe('My title');
  });
});
