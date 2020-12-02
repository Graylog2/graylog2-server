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
import { Map } from 'immutable';
import mockAction from 'helpers/mocking/MockAction';

import Widget from 'views/logic/widgets/Widget';
import RemoveFromAllTablesActionHandler from 'views/logic/fieldactions/RemoveFromAllTablesActionHandler';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import MessageWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { WidgetActions, WidgetStore } from 'views/stores/WidgetStore';

describe('RemoveFromAllTablesActionHandler', () => {
  it('should add a field to all message widgets', () => {
    const messageWidgetConfig = MessageWidgetConfig.builder()
      .fields(['timestamp', 'source', 'author'])
      .showMessageRow(true)
      .build();
    const messageWidget = Widget.builder()
      .newId()
      .type('MESSAGES')
      .config(messageWidgetConfig)
      .build();
    const pivotWidget = Widget.builder()
      .newId()
      .type('PIVOT')
      .build();
    const widgets = Map([[messageWidget.id, messageWidget], [pivotWidget.id, pivotWidget]]);

    const expectedMessageWidgetConfig = MessageWidgetConfig.builder()
      .fields(['timestamp', 'source'])
      .showMessageRow(true)
      .build();
    const expectedMessageWidget = Widget.builder()
      .id(messageWidget.id)
      .type('MESSAGES')
      .config(expectedMessageWidgetConfig)
      .build();

    const expectedWidgets = Map([[expectedMessageWidget.id, expectedMessageWidget], [pivotWidget.id, pivotWidget]]);

    // @ts-ignore
    WidgetStore.getInitialState = jest.fn(() => widgets);

    WidgetActions.updateWidgets = mockAction(jest.fn(async (newWidgets) => {
      expect(newWidgets).toEqual(expectedWidgets);

      return newWidgets;
    }));

    RemoveFromAllTablesActionHandler({ queryId: 'foo', field: 'author', type: FieldTypes.STRING(), contexts: {} });

    expect(WidgetActions.updateWidgets).toBeCalled();
  });
});
