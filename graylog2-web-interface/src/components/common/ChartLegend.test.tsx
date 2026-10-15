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
import userEvent from '@testing-library/user-event';

import ChartLegend from './ChartLegend';

const items = [
  { key: 'openai', label: 'OpenAI', color: '#4478b3' },
  { key: 'anthropic', label: 'Anthropic', color: '#fd9e48' },
];

describe('ChartLegend', () => {
  it('shows a color hint and a label per item', async () => {
    render(<ChartLegend items={items} />);

    await screen.findByText('OpenAI');
    await screen.findByText('Anthropic');

    expect(await screen.findAllByLabelText('Color Hint')).toHaveLength(2);
  });

  it('does not offer to change colors without a color picker', async () => {
    render(<ChartLegend items={items} />);

    await userEvent.click((await screen.findAllByLabelText('Color Hint'))[0]);

    expect(screen.queryByRole('heading', { name: /configuration for/i })).not.toBeInTheDocument();
  });

  it('lets the user change the color of an item', async () => {
    const onChange = jest.fn();

    render(<ChartLegend items={items} colorPicker={{ colors: [['#b71c1c', '#ce5246']], onChange }} />);

    await userEvent.click((await screen.findAllByLabelText('Color Hint'))[0]);
    await screen.findByRole('heading', { name: /configuration for openai/i });
    await userEvent.click(await screen.findByTitle('#b71c1c'));

    expect(onChange).toHaveBeenCalledWith('openai', '#b71c1c');
  });
});
