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
import { PluginStore } from 'graylog-web-plugin/plugin';
import asMock from 'helpers/mocking/AsMock';
import React from 'react';
import { EditWidgetComponentProps } from 'views/types';

import { WidgetProps } from 'views/components/widgets/Widget';
import Widget from 'views/logic/widgets/Widget';
import { createWidget } from 'views/logic/WidgetTestHelpers';

import { widgetDefinition } from './Widgets';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

const someTypeWidget = createWidget('some-type');
const someOtherTypeWidget = createWidget('some-other-type');
const defaultWidget = createWidget('default');

describe('Widgets', () => {
  describe('widgetDefinition', () => {
    it('returns widget definition if present', () => {
      asMock(PluginStore.exports).mockReturnValue([someOtherTypeWidget, someTypeWidget]);

      expect(widgetDefinition('some-type')).toEqual(someTypeWidget);
    });

    it('returns default definition if widget type is not present', () => {
      asMock(PluginStore.exports).mockReturnValue([someOtherTypeWidget, defaultWidget]);

      expect(widgetDefinition('some-type')).toEqual(defaultWidget);
    });

    it('throws error if widget type and default type are not present', () => {
      asMock(PluginStore.exports).mockReturnValue([someOtherTypeWidget]);

      expect(() => widgetDefinition('some-type'))
        .toThrowError('Neither a widget of type "some-type" nor a default widget are registered!');
    });
  });
});
