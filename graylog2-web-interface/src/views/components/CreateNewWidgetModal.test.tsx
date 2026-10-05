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
import type { PluginExports } from 'graylog-web-plugin/plugin';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useView from 'views/hooks/useView';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import { usePlugin } from 'views/test/testPlugins';
import Icon from 'components/common/Icon';
import { CreateMessageCount } from 'views/logic/fieldactions/AddMessageCountActionHandler';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import CreateNewWidgetModal from './CreateNewWidgetModal';

jest.mock('views/stores/useViewsDispatch');
jest.mock('views/hooks/useView');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('views/logic/slices/widgetActions', () => ({
  addWidget: jest.fn(() => async () => {}),
}));

const bindings: PluginExports = {
  widgetCreators: [
    {
      title: 'Message Count',
      func: CreateMessageCount,
      icon: () => <Icon name="tag" />,
    },
    {
      title: 'Custom Aggregation',
      func: CreateMessageCount,
      icon: () => <Icon name="monitoring" />,
    },
  ],
};

const plugin = {
  exports: bindings,
  metadata: {
    name: 'Dummy Plugin for Tests',
  },
};

describe('CreateNewWidgetModal', () => {
  const sendTelemetry = jest.fn();
  const onCancel = jest.fn();
  const position = WidgetPosition.builder().col(1).row(1).height(1).width(1).build();

  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useViewsDispatch).mockReturnValue(dispatch);
    asMock(useView).mockReturnValue(view);
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    sendTelemetry.mockClear();
  });

  usePlugin(plugin);

  it('sends telemetry with the matching event name for a widget type known to the telemetry constants', async () => {
    render(<CreateNewWidgetModal onCancel={onCancel} position={position} />);

    const button = await screen.findByRole('button', { name: /Create Message Count Widget/i });
    await userEvent.click(button);

    expect(sendTelemetry).toHaveBeenCalledWith('Search Widget Message Count Created', {});
  });

  it('falls back to a generated event name instead of sending an undefined event type for an unmapped widget type', async () => {
    render(<CreateNewWidgetModal onCancel={onCancel} position={position} />);

    const button = await screen.findByRole('button', { name: /Create Custom Aggregation Widget/i });
    await userEvent.click(button);

    expect(sendTelemetry).toHaveBeenCalledWith('Search Widget Custom Aggregation Created', {});
  });
});
