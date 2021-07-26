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

import FieldAndValueActionsContext, { FieldAndValueActionsContextType } from 'views/components/contexts/FieldAndValueActionsContext';
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

  type Props = Partial<React.ComponentProps<typeof Action>> & {
    fieldActions?: FieldAndValueActionsContextType['fieldActions'],
    valueActions?: FieldAndValueActionsContextType['valueActions'],
  }

  const SimpleAction = ({
    children = 'The dropdown header',
    handlerArgs = exampleHandlerArgs,
    menuContainer = undefined,
    type = 'field',
    fieldActions = { internal: undefined },
    valueActions = { internal: undefined, external: undefined },
  }: Props) => {
    return (
      <FieldAndValueActionsContext.Provider value={{ fieldActions, valueActions }}>
        <Action element={() => <div>Open Actions Menu</div>}
                handlerArgs={handlerArgs}
                menuContainer={menuContainer}
                type={type}>
          {children}
        </Action>
      </FieldAndValueActionsContext.Provider>
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
    const fieldActions = {
      internal: [
        {
          type: 'aggregate',
          title: 'Show top values',
          handler: mockActionHandler,
          isEnabled: () => true,
          resetFocus: true,
        },
      ],
    };

    render(<SimpleAction type="field" fieldActions={fieldActions} />);

    await openDropdown();

    const actionMenuItem = screen.getByText('Show top values');
    userEvent.click(actionMenuItem);

    expect(mockActionHandler).toHaveBeenCalledTimes(1);
  });

  it('should work with external value actions', async () => {
    const mockActionHandler = jest.fn();
    const simpleExternalAction = createSimpleExternalValueAction({ handler: mockActionHandler, title: 'External value action' });
    const valueActions = { external: [simpleExternalAction], internal: undefined };

    render(
      <SimpleAction type="value" valueActions={valueActions} />,
    );

    await openDropdown();

    const actionMenuItem = screen.getByText('External value action');
    userEvent.click(actionMenuItem);

    expect(mockActionHandler).toHaveBeenCalledTimes(1);
  });
});
