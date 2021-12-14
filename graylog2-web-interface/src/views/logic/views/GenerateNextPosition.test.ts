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
import * as Immutable from 'immutable';
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Widget from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { createWidget } from 'views/logic/WidgetTestHelpers';

import GenerateNextPosition from './GenerateNextPosition';

describe('GenerateNextPosition', () => {
  PluginStore.exports = (key: keyof PluginExports) => ({
    enterpriseWidgets: [{ ...createWidget('MESSAGES'), defaultHeight: 5, defaultWidth: 6 }],
  }[key]);

  it('should add a new widget to the first row and column', () => {
    const newMessageList = Widget.builder().id('foo-1').type('MESSAGES').build();
    const widgets = [newMessageList];
    const positions = Immutable.Map<string, WidgetPosition>();
    const newPositions = GenerateNextPosition(positions, widgets);

    expect(newPositions).toMatchSnapshot();
  });

  it('should add a new widget to the first row and column to other widgets', () => {
    const newMessageList = Widget.builder().id('foo-1').type('MESSAGES').build();
    const oldMessageList = Widget.builder().id('foo').type('MESSAGES').build();
    const oldWidgetPosition = WidgetPosition.builder()
      .col(1)
      .row(1)
      .width(3)
      .height(8)
      .build();
    const widgets = [newMessageList, oldMessageList];
    const positions = Immutable.Map({ foo: oldWidgetPosition });
    const newPositions = GenerateNextPosition(positions, widgets);

    expect(newPositions).toMatchSnapshot();
  });
});
