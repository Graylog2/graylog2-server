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

import Widget from 'views/logic/widgets/Widget';
import RemoveFromAllTablesActionHandler from 'views/logic/fieldactions/RemoveFromAllTablesActionHandler';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import MessageWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { mockDispatchForView } from 'views/test/mockDispatch';
import { updateWidgets } from 'views/logic/slices/widgetActions';
import { createViewWithWidgets } from 'fixtures/searches';

jest.mock('views/logic/slices/widgetActions', () => ({
  updateWidgets: jest.fn(),
}));

describe('RemoveFromAllTablesActionHandler', () => {
  it('should add a field to all message widgets', async () => {
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
    const widgets = [messageWidget, pivotWidget];

    const expectedMessageWidgetConfig = MessageWidgetConfig.builder()
      .fields(['timestamp', 'source'])
      .showMessageRow(true)
      .build();
    const expectedMessageWidget = Widget.builder()
      .id(messageWidget.id)
      .type('MESSAGES')
      .config(expectedMessageWidgetConfig)
      .build();

    const expectedWidgets = Immutable.List([expectedMessageWidget, pivotWidget]);
    const view = createViewWithWidgets(widgets, {});
    const dispatch = mockDispatchForView(view);

    await dispatch(RemoveFromAllTablesActionHandler({ queryId: 'foo', field: 'author', type: FieldTypes.STRING(), contexts: {} }));

    expect(updateWidgets).toHaveBeenCalledWith(expectedWidgets);
  });
});
