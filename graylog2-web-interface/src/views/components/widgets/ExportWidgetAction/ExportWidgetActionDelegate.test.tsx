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
import OriginalExportWidgetActionDelegate from 'views/components/widgets/ExportWidgetAction/ExportWidgetActionDelegate';
import useWidgetExportActionComponent from 'views/components/widgets/useWidgetExportActionComponent';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';

jest.mock('views/components/widgets/useWidgetExportActionComponent');
const renderExportWidgetActionDelegate = () =>
  render(<OriginalExportWidgetActionDelegate widget={AggregationWidget.empty()} />);

describe('ExtraMenuWidgetActions', () => {
  const findNativeButton = (buttons: Array<HTMLElement>) => buttons.find((button) => button.tagName === 'BUTTON');

  it('Render PNG export plug when there is no WidgetExportActionComponent', async () => {
    asMock(useWidgetExportActionComponent).mockReturnValue(null);

    renderExportWidgetActionDelegate();
    const exportButton = findNativeButton(await screen.findAllByRole('button', { name: /export widget/i }));

    expect(exportButton).toBeDefined();
    await userEvent.click(exportButton!);

    await screen.findByText(/png image/i);
  });

  it('Render original WidgetExportActionComponent without the plug', async () => {
    asMock(useWidgetExportActionComponent).mockReturnValue(() => (
      <button type="button" title="dummy export action">
        dummy export action
      </button>
    ));

    renderExportWidgetActionDelegate();

    expect(await screen.findByRole('button', { name: /dummy export action/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export widget/i })).not.toBeInTheDocument();
  });
});
