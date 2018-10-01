import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import Widget from './Widget';
import WidgetPosition from '../../logic/widgets/WidgetPosition';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';

jest.mock('../searchbar/QueryInput', () => mockComponent('QueryInput'));
jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => ([
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        visualizationComponent: 'dummy-visualization',
      },
    ]),
  },
}));

const createNodeMock = (element) => {
  if (element.type === 'div') {
    return {
      clientHeight: 420,
      clientWidth: 420,
    };
  }
  return null;
};

describe('<Widget />', () => {
  const widget = { config: {}, id: 'widgetId', type: 'dummy' };
  it('should render with empty props', () => {
    const options = { createNodeMock };
    const wrapper = renderer.create((
      <Widget widget={widget}
              id="widgetId"
              fields={[]}
              onPositionsChange={() => {}}
              onSizeChange={() => {}}
              title="Widget Title"
              position={new WidgetPosition(1, 1, 1, 1)} />
    ), options);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
  it('should render loading widget for widget without data', () => {
    const wrapper = mount((
      <Widget widget={widget}
              id="widgetId"
              fields={[]}
              onPositionsChange={() => {}}
              onSizeChange={() => {}}
              title="Widget Title"
              position={new WidgetPosition(1, 1, 1, 1)} />
    ));
    expect(wrapper.find(LoadingWidget)).toHaveLength(1);
  });
  it('should render error widget for widget with one error', () => {
    const wrapper = mount((
      <Widget widget={widget}
              errors={[{ description: 'The widget has failed: the dungeon collapsed, you die!' }]}
              id="widgetId"
              fields={[]}
              onPositionsChange={() => {}}
              onSizeChange={() => {}}
              title="Widget Title"
              position={new WidgetPosition(1, 1, 1, 1)} />
    ));
    const errorWidgets = wrapper.find(ErrorWidget);
    expect(errorWidgets).toHaveLength(1);
    expect(errorWidgets).toIncludeText('The widget has failed: the dungeon collapsed, you die!');
  });
  it('should render error widget including all error messages for widget with multiple errors', () => {
    const wrapper = mount((
      <Widget widget={widget}
              id="widgetId"
              errors={[
                { description: 'Something is wrong' },
                { description: 'Very wrong' },
              ]}
              fields={[]}
              onPositionsChange={() => {}}
              onSizeChange={() => {}}
              title="Widget Title"
              position={new WidgetPosition(1, 1, 1, 1)} />
    ));
    const errorWidgets = wrapper.find(ErrorWidget);
    expect(errorWidgets).toHaveLength(1);
    expect(errorWidgets).toIncludeText('Something is wrong');
    expect(errorWidgets).toIncludeText('Very wrong');
  });
  it('should render correct widget visualization for widget with data', () => {
    const wrapper = mount((
      <Widget widget={widget}
              id="widgetId"
              onSizeChange={() => {}}
              data={[]}
              fields={[]}
              title="Dummy Widget"
              onPositionsChange={() => {}}
              position={new WidgetPosition(1, 1, 1, 1)} />
    ));
    expect(wrapper.find(LoadingWidget)).toHaveLength(0);
    expect(wrapper.find('dummy-visualization')).toHaveLength(1);
  });
});
