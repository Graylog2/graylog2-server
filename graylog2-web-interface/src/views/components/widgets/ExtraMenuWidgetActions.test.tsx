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
import OriginalExtraMenuWidgetActions from 'views/components/widgets/ExtraMenuWidgetActions';
import Widget from 'views/logic/widgets/Widget';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useWidgetActions from 'views/components/widgets/useWidgetActions';
import type { WidgetActionType } from 'views/components/widgets/Types';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';

jest.mock('views/components/widgets/useWidgetActions');

const ExtraMenuWidgetActions = (props: React.ComponentProps<typeof OriginalExtraMenuWidgetActions>) => (
  <TestStoreProvider>
    <OriginalExtraMenuWidgetActions {...props} />
  </TestStoreProvider>
);

describe('ExtraMenuWidgetActions', () => {
  const widget = Widget.empty();
  const aggregationWidget = AggregationWidget.empty();
  const plugExplanation = /export aggregation widget feature is available for the enterprise version\. graylog provides option to export your data into most popular file formats such as csv, json, yaml, xml etc\./i;
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
  const dummyExportActionWithMenuPosition: WidgetActionType = {
    position: 'menu',
    type: 'export-widget-action',
    component: ({ disabled }) => <button type="button" title="dummy export action" disabled={disabled}>dummy export action</button>,
  };
  useViewsPlugin();

  it('Render export plug for aggregation widget when no widget export action provided', async () => {
    asMock(useWidgetActions).mockReturnValue([]);

    render(<ExtraMenuWidgetActions widget={aggregationWidget} />);
    const exportButton = await screen.findByRole('button', { name: /export widget/i });
    userEvent.click(exportButton);

    await screen.findByText(plugExplanation);
  });

  it('Do not render export plug for non aggregation widget when no widget export action provided', async () => {
    asMock(useWidgetActions).mockReturnValue([]);

    render(<ExtraMenuWidgetActions widget={widget} />);
    const exportButton = screen.queryByRole('button', { name: /export widget/i });

    expect(exportButton).toBeNull();
  });

  it('Render original export instead of plug for aggregation widget when widget export action provided', async () => {
    asMock(useWidgetActions).mockReturnValue([dummyExportActionWithMenuPosition]);

    render(<ExtraMenuWidgetActions widget={aggregationWidget} />);
    const exportButton = await screen.findByRole('button', { name: /dummy export action/i });
    userEvent.click(exportButton);

    const plugText = screen.queryByText(plugExplanation);

    expect(plugText).toBeNull();
  });

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
