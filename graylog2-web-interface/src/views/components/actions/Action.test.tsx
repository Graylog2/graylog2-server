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

import { createSimpleExternalValueAction } from 'fixtures/externalValueActions';
import type { ActionContexts, RootState } from 'views/types';
import asMock from 'helpers/mocking/AsMock';
import FieldType from 'views/logic/fieldtypes/FieldType';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import mockDispatch from 'views/test/mockDispatch';
import { createSearch } from 'fixtures/searches';
import useExternalValueActions from 'views/hooks/useExternalValueActions';
import FieldActionsContext, { type FieldActionsContextValue } from 'views/components/actions/FieldActionsContext';

import Action from './Action';

jest.mock('views/stores/useViewsDispatch');

jest.mock('views/hooks/useExternalValueActions');

describe('Action', () => {
  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useViewsDispatch).mockReturnValue(dispatch);

    asMock(useExternalValueActions).mockReturnValue({
      isLoading: false,
      externalValueActions: [],
      isError: false,
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const exampleHandlerArgs = {
    queryId: 'query-id',
    field: 'field1',
    value: 'field-value',
    type: new FieldType('string', [], []),
    contexts: {} as ActionContexts,
  };

  const actionsContextValue = {
    evaluateCondition: () => true,
    executeThunkAction: () => Promise.resolve(),
    additionalHandlerArgs: {},
    valueActions: [],
    fieldActions: [],
  };

  type Props = Partial<React.ComponentProps<typeof Action>> & {
    actionConfig?: FieldActionsContextValue;
  };

  const OpenActionsMenu = () => <div>Open Actions Menu</div>;

  const SimpleAction = ({
    children = 'The dropdown header',
    handlerArgs = exampleHandlerArgs,
    menuContainer = undefined,
    type = 'field',
    actionConfig = actionsContextValue,
  }: Props) => (
    <FieldActionsContext.Provider value={actionConfig}>
      <Action element={OpenActionsMenu} handlerArgs={handlerArgs} menuContainer={menuContainer} type={type}>
        {children}
      </Action>
    </FieldActionsContext.Provider>
  );

  const openDropdown = async (headerTitle = 'The dropdown header') => {
    const dropdownToggle = screen.getByText('Open Actions Menu');
    await userEvent.click(dropdownToggle);
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

    render(<SimpleAction type="field" actionConfig={{ ...actionsContextValue, fieldActions }} />);

    await openDropdown();

    const actionMenuItem = screen.getByText('Show top values');
    await userEvent.click(actionMenuItem);

    expect(mockActionHandler).toHaveBeenCalledTimes(1);
  });

  it('does not fail when no internal actions are configured', async () => {
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

    render(<SimpleAction type="value" />);

    await openDropdown();

    const actionMenuItem = (await screen.findByRole('menuitem', {
      name: /external value action/i,
    })) as HTMLAnchorElement;

    expect(actionMenuItem.href).toContain('the-link-to-field1');
  });
});
