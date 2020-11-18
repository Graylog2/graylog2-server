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
// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import EmptyEntity from './EmptyEntity';

describe('<EmptyEntity />', () => {
  it('should render children and default title', () => {
    const { getByText } = render(<EmptyEntity>The children</EmptyEntity>);

    expect(getByText('Looks like there is nothing here, yet!')).not.toBeNull();
    expect(getByText('The children')).not.toBeNull();
  });

  it('should render custom title', () => {
    const { getByText } = render(<EmptyEntity title="The custom title">The children</EmptyEntity>);

    expect(getByText('The custom title')).not.toBeNull();
  });
});
