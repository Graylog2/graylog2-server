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
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import OriginalExtraWidgetActions from 'views/components/widgets/ExtraWidgetActions';
import Widget from 'views/logic/widgets/Widget';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useWidgetActions from 'views/components/widgets/useWidgetActions';
import wrapWithMenu from 'helpers/components/wrapWithMenu';

jest.mock('views/components/widgets/useWidgetActions');

const ExtraWidgetActionsWithoutMenu = (props: React.ComponentProps<typeof OriginalExtraWidgetActions>) => (
  <TestStoreProvider>
    <OriginalExtraWidgetActions {...props} />
  </TestStoreProvider>
);

const ExtraWidgetActions = wrapWithMenu(ExtraWidgetActionsWithoutMenu);

describe('ExtraWidgetActions', () => {
  const widget = Widget.empty();
  const dummyActionWithoutIsHidden = {
    type: 'dummy-action',
    title: () => 'Dummy Action',
    action: jest.fn(() => async () => {}),
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

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  it('returns `null` if no action is configured', () => {
    asMock(useWidgetActions).mockReturnValue([]);

    const { container } = render(<ExtraWidgetActionsWithoutMenu widget={widget} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('returns `null` if no action is not hidden', () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsHidden]);

    const { container } = render(<ExtraWidgetActionsWithoutMenu widget={widget} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('renders action which has no `isHidden`', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWithoutIsHidden]);

    render(<ExtraWidgetActions widget={widget} />);

    await screen.findByText('Dummy Action');
  });

  it('renders action where `isHidden` returns `false`', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsNotHidden]);

    render(<ExtraWidgetActions widget={widget} />);

    await screen.findByText('Dummy Action');
  });

  it('clicking menu item triggers action', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsNotHidden]);

    render(<ExtraWidgetActions widget={widget} />);

    const menuItem = await screen.findByText('Dummy Action');

    await userEvent.click(menuItem);

    await waitFor(() => expect(dummyActionWhichIsNotHidden.action)
      .toHaveBeenCalledWith(widget, expect.objectContaining({
        widgetFocusContext: expect.objectContaining({
          focusedWidget: undefined,
          setWidgetFocusing: expect.any(Function),
          setWidgetEditing: expect.any(Function),
        }),
      })));
  });

  it('renders divider if at least one action is present', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWithoutIsHidden]);

    render(<ExtraWidgetActions widget={widget} />);

    await screen.findByRole('separator');
  });

  it('renders a disabled action disabled', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyActionWhichIsDisabled]);

    render(<ExtraWidgetActions widget={widget} />);

    const menuItem = await screen.findByRole('menuitem');

    expect(menuItem).toBeDisabled();
  });
});
