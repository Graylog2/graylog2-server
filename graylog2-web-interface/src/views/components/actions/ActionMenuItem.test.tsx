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

import { alice } from 'fixtures/users';
import FieldType from 'views/logic/fieldtypes/FieldType';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import { asMock } from 'helpers/mocking';
import useAppDispatch from 'stores/useAppDispatch';
import wrapWithMenu from 'helpers/components/wrapWithMenu';

import OriginalActionMenuItem from './ActionMenuItem';

jest.mock('stores/useAppDispatch');

const ActionMenuItem = wrapWithMenu(OriginalActionMenuItem);

describe('ActionMenuItem', () => {
  const baseAction = {
    isEnabled: () => true,
    isHidden: () => false,
    resetFocus: false,
    title: 'The action title',
    type: 'hashWatchlist',
  };

  const handlerArgs = {
    queryId: 'query-id',
    field: 'field-name',
    value: 'field-value',
    type: new FieldType('string', [], []),
    contexts: {
      view: View.create(),
      analysisDisabledFields: [],
      currentUser: alice,
      widget: Widget.empty(),
      message: {
        id: 'message-id',
        index: 'index',
        fields: [],
      },
      valuePath: [],
      isLocalNode: true,
    },
  };

  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useAppDispatch).mockReturnValue(dispatch);
  });

  it('should display help text for actions with handler', () => {
    const action = {
      ...baseAction,
      handler: () => Promise.resolve(),
      help: () => ({ title: 'The help title', description: 'The help description' }),
    };

    render(<ActionMenuItem action={action}
                           handlerArgs={handlerArgs}
                           onMenuToggle={() => {}}
                           overflowingComponents={{ foo: <span /> }}
                           setOverflowingComponents={() => {}}
                           type="value" />);

    expect(screen.getByTestId('menu-item-help')).toBeInTheDocument();
  });

  it('should display help text for external links', () => {
    const action = {
      ...baseAction,
      linkTarget: () => '/the-link',
      help: () => ({ title: 'The help title', description: 'The help description' }),
    };

    render(<ActionMenuItem action={action}
                           handlerArgs={handlerArgs}
                           onMenuToggle={() => {}}
                           overflowingComponents={{ foo: <span /> }}
                           setOverflowingComponents={() => {}}
                           type="value" />);

    expect(screen.getByTestId('menu-item-help')).toBeInTheDocument();
  });
});
