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
import asMock from 'helpers/mocking/AsMock';
import { createSimpleExternalValueAction } from 'fixtures/externalValueActions';

import ExternalValueActionsContext from 'views/components/contexts/ExternalValueActionsContext';
import usePluginEntities from 'views/logic/usePluginEntities';
import FieldType from 'views/logic/fieldtypes/FieldType';

import Action from './Action';

jest.mock('views/logic/usePluginEntities', () => jest.fn(() => []));

describe('Action', () => {
  const exampleHandlerArgs = {
    queryId: 'query-id',
    field: 'field1',
    value: 'field-value',
    type: new FieldType('string', [], []),
    contexts: {},
  };

  const SimpleAction = ({
    children = 'The dropdown header',
    handlerArgs = exampleHandlerArgs,
    menuContainer = undefined,
    type = 'field',
  }: Partial<React.ComponentProps<typeof Action>>) => {
    return (
      <>
        <Action element={() => <div>Open Actions Menu</div>}
                handlerArgs={handlerArgs}
                menuContainer={menuContainer}
                type={type}>
          {children}
        </Action>
      </>
    );
  };

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

    asMock(usePluginEntities).mockImplementation((entityKey) => {
      if (entityKey === 'fieldActions') {
        return [{
          type: 'aggregate',
          title: 'Show top values',
          handler: mockActionHandler,
          isEnabled: () => true,
          resetFocus: true,
        }];
      }

      return [];
    });

    render(<SimpleAction type="field" />);

    await openDropdown();

    const actionMenuItem = screen.getByText('Show top values');
    userEvent.click(actionMenuItem);

    expect(mockActionHandler).toHaveBeenCalledTimes(1);
  });

  it('should work with external value actions', async () => {
    const mockActionHandler = jest.fn();
    const simpleExternalAction = createSimpleExternalValueAction({ handler: mockActionHandler, title: 'External value action' });

    render(
      <ExternalValueActionsContext.Provider value={{ externalValueActions: [simpleExternalAction] }}>
        <SimpleAction type="value" />
      </ExternalValueActionsContext.Provider>,
    );

    await openDropdown();

    const actionMenuItem = screen.getByText('External value action');
    userEvent.click(actionMenuItem);

    expect(mockActionHandler).toHaveBeenCalledTimes(1);
  });
});
