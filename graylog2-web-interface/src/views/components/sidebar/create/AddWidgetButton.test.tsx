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
import { render, screen } from 'wrappedTestingLibrary';
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useAppDispatch from 'stores/useAppDispatch';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import Button from 'components/bootstrap/Button';

import AddWidgetButton from './AddWidgetButton';

const mockAggregateActionHandler = jest.fn();
const mockAddMessageCountActionHandler = jest.fn();
const mockAddMessageTableActionHandler = jest.fn();

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

jest.mock('stores/useAppDispatch');

describe('AddWidgetButton', () => {
  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useAppDispatch).mockReturnValue(dispatch);
    PluginStore.register(plugin);
  });

  afterEach(() => {
    PluginStore.unregister(plugin);
  });

  const onClick = jest.fn();

  it('contains menu items for all widget types', async () => {
    render(<AddWidgetButton onClick={onClick} />);
    await screen.findByText(/Use the following options to add an aggregation/i);

    ['Aggregation', 'Message Count', 'Message Table', 'Parameter']
      .forEach((title) => expect(screen.getByRole('button', { name: title })).toBeInTheDocument());
  });

  it('clicking on option to add aggregation calls AggregateActionHandler', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addAggregation = await screen.findByRole('button', { name: 'Aggregation' });

    addAggregation.click();

    expect(mockAggregateActionHandler).toHaveBeenCalled();
  });

  it('clicking on option to add message count calls AddMessageCountActionHandler', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addMessageCount = await screen.findByRole('button', { name: 'Message Count' });

    addMessageCount.click();

    expect(mockAddMessageCountActionHandler).toHaveBeenCalled();
  });

  it('clicking on option to add message table calls AddMessageTableActionHandler', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addMessageTable = await screen.findByRole('button', { name: 'Message Table' });

    addMessageTable.click();

    expect(mockAddMessageTableActionHandler).toHaveBeenCalled();
  });

  it('clicking on option to add a parameter renders MockCreateParameterDialog', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addParameter = await screen.findByRole('button', { name: 'Parameter' });

    addParameter.click();

    await screen.findByRole('button', { name: '42' });
  });

  it('calling onClose from creator component removes it', async () => {
    render(<AddWidgetButton onClick={onClick} />);

    const addParameter = await screen.findByRole('button', { name: 'Parameter' });

    addParameter.click();

    const mockDialogButton = await screen.findByRole('button', { name: '42' });
    mockDialogButton.click();

    expect(screen.queryByRole('button', { name: '42' })).not.toBeInTheDocument();
  });
});
