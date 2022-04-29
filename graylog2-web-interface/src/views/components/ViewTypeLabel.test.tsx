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
import React from 'react';
import { render } from 'wrappedTestingLibrary';

import View from 'views/logic/views/View';

import ViewTypeLabel from './ViewTypeLabel';

describe('ViewTypeLabel', () => {
  it('should create correct label for view type search', () => {
    const viewTypeLabel = ViewTypeLabel({ type: View.Type.Search });
    const { getByText } = render(<div>{viewTypeLabel}</div>);

    expect(getByText('search')).not.toBe(null);
  });

  it('should create correct label for view type dasboard', () => {
    const viewTypeLabel = ViewTypeLabel({ type: View.Type.Dashboard });
    const { getByText } = render(<div>{viewTypeLabel}</div>);

    expect(getByText('dashboard')).not.toBe(null);
  });

  it('should create capitalized label', () => {
    const viewTypeLabel = ViewTypeLabel({ type: View.Type.Search, capitalize: true });
    const { getByText } = render(<div>{viewTypeLabel}</div>);

    expect(getByText('Search')).not.toBe(null);
  });
});
