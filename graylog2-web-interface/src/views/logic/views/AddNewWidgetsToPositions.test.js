// @flow strict
import * as Immutable from 'immutable';

import { PluginStore } from 'graylog-web-plugin/plugin';
import Widget from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import AddNewWidgetsToPositions from './AddNewWidgetsToPositions';

describe('AddNewWidgetsToPositions', () => {
  PluginStore.exports = () => {
    return [{ type: 'MESSAGES', defaultHeight: 5, defaultWidth: 6 }];
  };
  it('should add a new widget to the first row and column', () => {
    const newMessageList = Widget.builder().id('foo-1').type('MESSAGES').build();
    const widgets = [newMessageList];
    const positions = Immutable.Map();
    const newPositions = AddNewWidgetsToPositions(positions, widgets);

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
    const newPositions = AddNewWidgetsToPositions(positions, widgets);

    expect(newPositions).toMatchSnapshot();
  });
});
