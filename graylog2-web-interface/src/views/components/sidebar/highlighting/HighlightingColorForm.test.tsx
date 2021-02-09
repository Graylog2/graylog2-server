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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import HighlightingColorForm from 'views/components/sidebar/highlighting/HighlightingColorForm';
import { GradientColor, StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';

describe('HighlightingColorForm', () => {
  const field = FieldTypeMapping.create('foo', FieldType.create('number', [Properties.Numeric]));

  it('selects correct type for static color', async () => {
    const staticColor = StaticColor.create('#666666');
    render(<HighlightingColorForm name="color" field={field} value={staticColor} onChange={jest.fn()} />);

    const staticOption = await screen.findByLabelText('Static Color');

    expect(staticOption).toBeChecked();
  });

  it('selects correct type for gradient', async () => {
    const gradientColor = GradientColor.create('Viridis', 0, 100);
    render(<HighlightingColorForm name="color" field={field} value={gradientColor} onChange={jest.fn()} />);

    const gradientOption = await screen.findByLabelText('Gradient');

    expect(gradientOption).toBeChecked();

    await screen.findByText('Viridis');
  });

  it('should not select a type if none is present', async () => {
    render(<HighlightingColorForm name="color" field={field} value={undefined} onChange={jest.fn()} />);
    const staticOption = await screen.findByLabelText('Static Color');

    expect(staticOption).not.toBeChecked();

    const gradientOption = await screen.findByLabelText('Gradient');

    expect(gradientOption).not.toBeChecked();
  });

  it('disables gradient option for non-numeric types', async () => {
    const nonNumericField = FieldTypeMapping.create('foo', FieldType.create('string', []));

    render(<HighlightingColorForm name="color" field={nonNumericField} value={undefined} onChange={jest.fn()} />);
    const staticOption = await screen.findByLabelText('Static Color');

    expect(staticOption).not.toBeDisabled();

    const gradientOption = await screen.findByLabelText('Gradient');

    expect(gradientOption).toBeDisabled();
  });

  it('assigns a new static color when type is selected', async () => {
    const onChange = jest.fn();
    render(<HighlightingColorForm name="color" field={field} value={undefined} onChange={onChange} />);
    userEvent.click(screen.getByLabelText('Static Color'));

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      target: expect.objectContaining({
        name: 'color',
        value: expect.objectContaining({ color: expect.any(String) }),
      }),
    })));
  });

  it('creates a new gradient when type is selected', async () => {
    const onChange = jest.fn();
    render(<HighlightingColorForm name="color" field={field} value={undefined} onChange={onChange} />);
    userEvent.click(screen.getByLabelText('Gradient'));

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      target: expect.objectContaining({
        name: 'color',
        value: expect.objectContaining({ gradient: 'Viridis' }),
      }),
    })));
  });
});
