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
import { PluginStore } from 'graylog-web-plugin/plugin';
import { createSimpleExternalValueAction } from 'fixtures/externalValueActions';
import asMock from 'helpers/mocking/AsMock';

import ExternalValueActionsProvider from './ExternalValueActionsProvider';
import ExternalValueActionsContext from './ExternalValueActionsContext';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

const simpleValueAction = createSimpleExternalValueAction();
const simpleContextValue = {
  externalValueActions: [simpleValueAction],
};

describe('ExternalValueActionsProvider', () => {
  const renderSUT = (consume) => render(
    <ExternalValueActionsProvider>
      <ExternalValueActionsContext.Consumer>
        {consume}
      </ExternalValueActionsContext.Consumer>
    </ExternalValueActionsProvider>,
  );

  it('renders children when there are no external value actions in plugin store', () => {
    render(<ExternalValueActionsProvider><>The content</></ExternalValueActionsProvider>);

    expect(screen.getByText('The content')).toBeInTheDocument();
  });

  it('provides external value actions from plugin store', () => {
    asMock(PluginStore.exports).mockImplementation((type) => ({
      externalValueActions: [[simpleValueAction]],
    }[type]));

    let contextValue;

    renderSUT((value) => {
      contextValue = value;
    });

    expect(contextValue.externalValueActions).toStrictEqual(simpleContextValue.externalValueActions);
  });

  it('provides correct data, when plugin store contains multiple entries for external value actions', () => {
    const simpleValueAction2 = createSimpleExternalValueAction({ title: 'External Value Action 2' });

    asMock(PluginStore.exports).mockImplementation((type) => ({
      externalValueActions: [
        [simpleValueAction],
        [simpleValueAction2],
      ],
    }[type]));

    let contextValue;

    renderSUT((value) => {
      contextValue = value;
    });

    const expectedContextValue = {
      externalValueActions: [simpleValueAction, simpleValueAction2],
    };

    expect(contextValue.externalValueActions).toStrictEqual(expectedContextValue.externalValueActions);
  });
});
