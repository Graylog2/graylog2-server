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

import HighlightingColorForm from 'views/components/sidebar/highlighting/HighlightingColorForm';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

describe('HighlightingColorForm', () => {
  it('shows color picker for static color', async () => {
    const color = StaticColor.create('#666666');
    render(<HighlightingColorForm name="color" value={color} onChange={jest.fn()} />);

    const typeInput = await screen.findByLabelText('Static Color');

    expect(typeInput).toBeChecked();
  });
});
