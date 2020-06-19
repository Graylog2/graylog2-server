// @flow strict
import { Map } from 'immutable';
import mockAction from 'helpers/mocking/MockAction';

import Widget from 'views/logic/widgets/Widget';
import AddToAllTablesActionHandler from 'views/logic/fieldactions/AddToAllTablesActionHandler';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import MessageWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { WidgetActions, WidgetStore } from 'views/stores/WidgetStore';

describe('AddToAllTablesActionHandler', () => {
  it('should add a field to all message widgets', () => {
    const messageWidgetConfig = MessageWidgetConfig.builder()
      .fields(['timestamp', 'source'])
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
      .fields(['timestamp', 'source', 'author'])
      .showMessageRow(true)
      .build();
    const expectdMessageWidget = Widget.builder()
      .id(messageWidget.id)
      .type('MESSAGES')
      .config(expectedMessageWidgetConfig)
      .build();

    const expectedWidgets = Map([[expectdMessageWidget.id, expectdMessageWidget], [pivotWidget.id, pivotWidget]]);

    WidgetStore.getInitialState = jest.fn(() => widgets);
    WidgetActions.updateWidgets = mockAction(jest.fn((newWidgets) => {
      expect(newWidgets).toEqual(expectedWidgets);
      return Promise.resolve();
    }));
    AddToAllTablesActionHandler({ queryId: 'foo', field: 'author', type: FieldTypes.STRING(), contexts: {} });
    expect(WidgetActions.updateWidgets).toBeCalled();
  });
});
