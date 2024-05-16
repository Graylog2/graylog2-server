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

import asMock from 'helpers/mocking/AsMock';
import OriginalExtraMenuWidgetActions from 'views/components/widgets/ExtraMenuWidgetActions';
import Widget from 'views/logic/widgets/Widget';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useWidgetActions from 'views/components/widgets/useWidgetActions';
import type { WidgetActionType } from 'views/components/widgets/Types';

jest.mock('views/components/widgets/useWidgetActions');

const ExtraMenuWidgetActions = (props: React.ComponentProps<typeof OriginalExtraMenuWidgetActions>) => (
  <TestStoreProvider>
    <OriginalExtraMenuWidgetActions {...props} />
  </TestStoreProvider>
);

describe('ExtraMenuWidgetActions', () => {
  const widget = Widget.empty();
  const dummyActionWithoutIsHidden: WidgetActionType = {
    position: 'menu',
    type: 'dummy-action',
    component: ({ disabled }) => <button type="button" title="dummy action" disabled={disabled}>dummy action</button>,
  };
  const dummyActionWhichIsHidden = {
    ...dummyActionWithoutIsHidden,
    isHidden: jest.fn(() => true),
  };
  const dummyActionWhichIsNotHidden = {
    ...dummyActionWithoutIsHidden,
    isHidden: jest.fn(() => false),
  };
  const dummyActionWhichIsDisabled = {
    ...dummyActionWithoutIsHidden,
    disabled: jest.fn(() => true),
  };
  const dummyActionWithDropdownPosition: WidgetActionType = {
    ...dummyActionWithoutIsHidden,
    position: 'dropdown',
  };
  useViewsPlugin();

  it('renders action which has no `isHidden`', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWithoutIsHidden]);

    render(<ExtraMenuWidgetActions widget={widget} />);
    await screen.findByRole('button', { name: /dummy action/i });
  });

  it('renders action where `isHidden` returns `false`', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsNotHidden]);

    render(<ExtraMenuWidgetActions widget={widget} />);

    await screen.findByRole('button', { name: /dummy action/i });
  });

  it('use disabled props from action in component', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsDisabled]);

    render(<ExtraMenuWidgetActions widget={widget} />);
    const actionButton = await screen.findByRole('button', { name: /dummy action/i });

    expect(actionButton).toBeDisabled();
  });

  it('does not render menu items, when action has dropdown position', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWithDropdownPosition]);

    render(<ExtraMenuWidgetActions widget={widget} />);

    const actionButton = screen.queryByRole('button', { name: /dummy action/i });

    expect(actionButton).toBeNull();
  });

  it('does not render hidden item', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsHidden]);

    render(<ExtraMenuWidgetActions widget={widget} />);

    const actionButton = screen.queryByRole('button', { name: /dummy action/i });

    expect(actionButton).toBeNull();
  });
});
