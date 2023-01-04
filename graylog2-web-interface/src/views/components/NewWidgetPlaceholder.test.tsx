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

import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import NewWidgetPlaceholder from './NewWidgetPlaceholder';

describe('NewWidgetPlaceholder', () => {
  const widgetPosition = WidgetPosition.builder()
    .col(3)
    .row(3)
    .height(4)
    .width(8)
    .build();

  it('shows helpful text when rendered', async () => {
    render(<NewWidgetPlaceholder position={widgetPosition} component={() => null} />);
    await screen.findByText('Create a new widget here');
  });

  it('renders custom component when clicked', async () => {
    const component = () => <>Hey there!</>;
    render(<NewWidgetPlaceholder position={widgetPosition} component={component} />);
    const text = await screen.findByText('Create a new widget here');
    userEvent.click(text);

    await screen.findByText('Hey there!');
  });

  it('passes position to custom component', async () => {
    const component = jest.fn(() => <>Hey there!</>);
    render(<NewWidgetPlaceholder position={widgetPosition} component={component} />);
    const text = await screen.findByText('Create a new widget here');
    userEvent.click(text);

    await screen.findByText('Hey there!');

    expect(component).toHaveBeenCalledWith(expect.objectContaining({ position: widgetPosition }), expect.anything());
  });

  it('unmounts custom component after calling `onCancel`', async () => {
    const component = ({ onCancel }) => <button type="button" onClick={onCancel}>Close</button>;
    render(<NewWidgetPlaceholder position={widgetPosition} component={component} />);
    const text = await screen.findByText('Create a new widget here');
    userEvent.click(text);

    const close = await screen.findByRole('button', { name: 'Close' });
    userEvent.click(close);

    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
  });
});
