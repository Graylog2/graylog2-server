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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import React from 'react';

import WidgetContext from 'views/components/contexts/WidgetContext';
import { asMock } from 'helpers/mocking';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import Widget from 'views/logic/widgets/Widget';

import useSendFavoriteFieldTelemetry from './useSendFavoriteFieldTelemetry';

jest.mock('logic/telemetry/useSendTelemetry');

describe('useSendFavoriteFieldTelemetry', () => {
  const mockSendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendTelemetry).mockReturnValue(mockSendTelemetry);
  });

  it('should send telemetry with widget type when widget context exists', () => {
    const mockWidget = Widget.builder().id('widget-id').type('MESSAGES').config({}).build();

    const wrapper = ({ children }) => <WidgetContext.Provider value={mockWidget}>{children}</WidgetContext.Provider>;

    const { result } = renderHook(() => useSendFavoriteFieldTelemetry(), { wrapper });

    result.current('TOGGLED', { app_action_value: 'add' });

    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.FAVORITE_FIELDS.TOGGLED, {
      app_section: 'MESSAGES',
      app_action_value: 'add',
    });
  });

  it('should send telemetry without widget type when widget context is permalink', () => {
    const wrapper = ({ children }) => <WidgetContext.Provider value={undefined}>{children}</WidgetContext.Provider>;

    const { result } = renderHook(() => useSendFavoriteFieldTelemetry(), { wrapper });

    result.current('TOGGLED', { app_action_value: 'remove' });

    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.FAVORITE_FIELDS.TOGGLED, {
      app_section: 'permalink',
      app_action_value: 'remove',
    });
  });

  it('should send telemetry without extra parameters when not provided', () => {
    const mockWidget = Widget.builder().id('widget-id').type('AGGREGATION').config({}).build();

    const wrapper = ({ children }) => <WidgetContext.Provider value={mockWidget}>{children}</WidgetContext.Provider>;

    const { result } = renderHook(() => useSendFavoriteFieldTelemetry(), { wrapper });

    result.current('EDIT_SAVED');

    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.FAVORITE_FIELDS.EDIT_SAVED, {
      app_section: 'AGGREGATION',
    });
  });

  it('should handle missing widget context gracefully (permalink scenario)', () => {
    // Simulate the permalink scenario where there's no WidgetContext.Provider at all
    const { result } = renderHook(() => useSendFavoriteFieldTelemetry());

    // This should not throw an error
    expect(() => {
      result.current('TOGGLED', { app_action_value: 'add' });
    }).not.toThrow();

    expect(mockSendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.FAVORITE_FIELDS.TOGGLED, {
      app_section: 'permalink',
      app_action_value: 'add',
    });
  });
});
