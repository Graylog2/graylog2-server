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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import Label from './Label';

describe('Label', () => {
  it('renders bsStyle-driven content', () => {
    render(<Label bsStyle="success">Running</Label>);

    expect(screen.getByText('Running')).toBeInTheDocument();
  });

  it('keeps the legacy small border-radius', () => {
    render(
      <Label bsStyle="success" data-testid="label">
        Legacy
      </Label>,
    );

    expect(screen.getByTestId('label')).toHaveStyleRule('border-radius', '3px');
  });
});
