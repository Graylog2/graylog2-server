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
const renderExportWidgetActionDelegate = () => render(
  <OriginalExportWidgetActionDelegate widget={AggregationWidget.empty()} />,
);

describe('ExtraMenuWidgetActions', () => {
  const plugExplanation = /export aggregation widget feature is available for the enterprise version\. graylog provides option to export your data into most popular file formats such as csv, json, yaml, xml etc\./i;

  it('Render plug when there is no WidgetExportActionComponent', async () => {
    asMock(useWidgetExportActionComponent).mockReturnValue(null);

    renderExportWidgetActionDelegate();
    const exportButton = await screen.findByRole('button', { name: /export widget/i });
    userEvent.click(exportButton);

    const plugText = screen.queryByText(plugExplanation);

    expect(plugText).toBeNull();
  });

  it('Render original WidgetExportActionComponent without a plug', async () => {
    asMock(useWidgetExportActionComponent).mockReturnValue(() => <button type="button" title="dummy export action">dummy export action</button>);

    renderExportWidgetActionDelegate();
    const exportButton = await screen.findByRole('button', { name: /dummy export action/i });
    const plugExportButton = screen.queryByRole('button', { name: /export widget/i });
    userEvent.click(exportButton);

    const plugText = screen.queryByText(plugExplanation);

    expect(plugText).toBeNull();
    expect(plugExportButton).toBeNull();
  });
});
