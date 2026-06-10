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
import type { PluginExports } from 'graylog-web-plugin/plugin';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import Button from 'components/bootstrap/Button';
import { usePlugin } from 'views/test/testPlugins';

import AddWidgetButton from './AddWidgetButton';

const dummyActionFunc = () => async () => 'newWidget';
const mockAggregateActionHandler = jest.fn(dummyActionFunc);
const mockAddMessageCountActionHandler = jest.fn(dummyActionFunc);
const mockAddMessageTableActionHandler = jest.fn(dummyActionFunc);

const MockCreateParameterDialog = ({ onClose }: { onClose: () => void }) => <Button onClick={onClose}>42</Button>;

const bindings: PluginExports = {
  creators: [
    {
      type: 'preset',
      title: 'Message Count',
      func: mockAddMessageCountActionHandler,
    },
    {
      type: 'preset',
      title: 'Message Table',
      func: mockAddMessageTableActionHandler,
    },
    {
      type: 'generic',
      title: 'Aggregation',
      func: mockAggregateActionHandler,
    },
    {
      type: 'generic',
      title: 'Parameter',
      component: MockCreateParameterDialog,
    },
  ],
};

const plugin = {
  exports: bindings,
  metadata: {
    name: 'Dummy Plugin for Tests',
  },
};

jest.mock('views/stores/useViewsDispatch');

describe('AddWidgetButton', () => {
  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useViewsDispatch).mockReturnValue(dispatch);
  });

  usePlugin(plugin);

  const onClick = jest.fn();

  it('contains menu items for all widget types', async () => {
    render(<AddWidgetButton onClick={onClick} />);
    await screen.findByText(/Use the following options to add an aggregation/i);

    ['Aggregation', 'Message Count', 'Message Table', 'Parameter'].forEach((title) =>
      expect(screen.getByRole('button', { name: title })).toBeInTheDocument(),
    );
  });

  it.each`
    option             | handler
    ${'Aggregation'}   | ${mockAggregateActionHandler}
    ${'Message Count'} | ${mockAddMessageCountActionHandler}
    ${'Message Table'} | ${mockAddMessageTableActionHandler}
  `(`clicking on "$option" calls respective handler`, async ({ option, handler }) => {
    render(<AddWidgetButton onClick={onClick} />);

    const addWidget = await screen.findByRole('button', { name: option });

    await userEvent.click(addWidget);

    expect(handler).toHaveBeenCalled();
  });

  it('clicking on option to add a parameter renders MockCreateParameterDialog', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addParameter = await screen.findByRole('button', { name: 'Parameter' });

    await userEvent.click(addParameter);

    await screen.findByRole('button', { name: '42' });
  });

  it('calling onClose from creator component removes it', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addParameter = await screen.findByRole('button', { name: 'Parameter' });

    await userEvent.click(addParameter);

    const mockDialogButton = await screen.findByRole('button', { name: '42' });
    await userEvent.click(mockDialogButton);

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: '42' })).not.toBeInTheDocument();
    });
  });
});
