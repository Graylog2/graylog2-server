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
import noop from 'lodash/noop';

import { createSimpleExternalValueAction } from 'fixtures/externalValueActions';
import type { ActionContexts, RootState } from 'views/types';
import asMock from 'helpers/mocking/AsMock';
import usePluginEntities from 'hooks/usePluginEntities';
import FieldType from 'views/logic/fieldtypes/FieldType';
import useAppDispatch from 'stores/useAppDispatch';
import mockDispatch from 'views/test/mockDispatch';
import { createSearch } from 'fixtures/searches';
import useExternalValueActions from 'views/hooks/useExternalValueActions';

import Action from './Action';

jest.mock('hooks/usePluginEntities', () => jest.fn(() => []));
jest.mock('stores/useAppDispatch');

jest.mock('views/hooks/useExternalValueActions');

describe('Action', () => {
  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useAppDispatch).mockReturnValue(dispatch);

    asMock(useExternalValueActions).mockReturnValue({
      isLoading: false,
      externalValueActions: [],
      isError: false,
    });
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  const exampleHandlerArgs = {
    queryId: 'query-id',
    field: 'field1',
    value: 'field-value',
    type: new FieldType('string', [], []),
    contexts: {} as ActionContexts,
  };

  type Props = Partial<React.ComponentProps<typeof Action>>;

  const OpenActionsMenu = () => (<div>Open Actions Menu</div>);

  const SimpleAction = ({
    children = 'The dropdown header',
    handlerArgs = exampleHandlerArgs,
    menuContainer = undefined,
    type = 'field',
  }: Props) => (
    <Action element={OpenActionsMenu}
            handlerArgs={handlerArgs}
            menuContainer={menuContainer}
            type={type}>
      {children}
    </Action>
  );

  const openDropdown = async (headerTitle = 'The dropdown header') => {
    const dropdownToggle = screen.getByText('Open Actions Menu');
    userEvent.click(dropdownToggle);
    await screen.findByText(headerTitle);
  };

  it('should render dropdown header', async () => {
    render(<SimpleAction>The dropdown header</SimpleAction>);
    await openDropdown('The dropdown header');

    expect(screen.getByText('The dropdown header')).toBeInTheDocument();
  });

  it('should work with internal field actions', async () => {
    const mockActionHandler = jest.fn();
    const fieldActions = [
      {
        type: 'aggregate',
        title: 'Show top values',
        handler: mockActionHandler,
        isEnabled: () => true,
        resetFocus: true,
      },
    ];

    asMock(usePluginEntities).mockImplementation((entityKey) => ({ fieldActions }[entityKey]));

    render(<SimpleAction type="field" />);

    await openDropdown();

    const actionMenuItem = screen.getByText('Show top values');
    userEvent.click(actionMenuItem);

    expect(mockActionHandler).toHaveBeenCalledTimes(1);
  });

  it('does not fail when plugin is not present for external actions', async () => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({ wrongKey: noop }[entityKey]));

    render(<SimpleAction>The dropdown header</SimpleAction>);
    await openDropdown('The dropdown header');

    expect(screen.getByText('The dropdown header')).toBeInTheDocument();
  });

  it('should work with external value actions', async () => {
    const linkTarget = ({ field }) => `the-link-to-${field}`;
    const simpleExternalAction = createSimpleExternalValueAction({ title: 'External value action', linkTarget });
    const externalValueActions = [simpleExternalAction];

    asMock(useExternalValueActions).mockReturnValue({
      externalValueActions,
      isLoading: false,
      isError: false,
    });

    render(
      <SimpleAction type="value" />,
    );

    await openDropdown();

    const actionMenuItem = await screen.findByRole('menuitem', { name: /external value action/i }) as HTMLAnchorElement;

    expect(actionMenuItem.href).toContain('the-link-to-field1');
  });
});
