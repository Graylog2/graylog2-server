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

import { asMock } from 'helpers/mocking';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import Slicing from './Slicing';

jest.mock('logic/telemetry/useSendTelemetry');

describe('Slicing', () => {
  const columnSchemas = [
    { id: 'title', title: 'Title', type: 'STRING' as const, sliceable: true },
    { id: 'status', title: 'Status', type: 'STRING' as const, sliceable: true },
    { id: 'description', title: 'Description', type: 'STRING' as const, sliceable: false },
  ];

  const renderSUT = (props: Partial<React.ComponentProps<typeof Slicing>> = {}) => {
    const onChangeSlicing = jest.fn();

    asMock(useSendTelemetry).mockReturnValue(jest.fn());

    render(
      <Slicing
        appSection="test-app-section"
        sliceCol="status"
        columnSchemas={columnSchemas}
        onChangeSlicing={onChangeSlicing}
        {...props}
      />,
    );

    return { onChangeSlicing };
  };

  it('displays slice options', async () => {
    renderSUT();

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /title/i });
    await screen.findByRole('menuitem', { name: /status/i });
    expect(screen.queryByRole('menuitem', { name: /description/i })).not.toBeInTheDocument();
  });

  it('selects a slice', async () => {
    const { onChangeSlicing } = renderSUT();

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    const menuItem = await screen.findByRole('menuitem', { name: /title/i });
    await userEvent.click(menuItem);

    expect(onChangeSlicing).toHaveBeenCalledWith('title');
  });

  it('removes slicing', async () => {
    const { onChangeSlicing } = renderSUT();

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    const menuItem = await screen.findByRole('menuitem', { name: /no slicing/i });
    await userEvent.click(menuItem);

    expect(onChangeSlicing).toHaveBeenCalledWith(undefined, undefined);
  });
});
