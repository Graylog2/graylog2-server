import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'enterprise/stores/TitlesStore';
import WidgetPosition from 'enterprise/logic/widgets/WidgetPosition';

import Widget from './Widget';
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

jest.mock('enterprise/stores/WidgetStore');
jest.mock('enterprise/stores/TitlesStore');

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

  it('copies title when duplicating widget', (done) => {
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

    wrapper.find('WidgetActionToggle').simulate('click');
    const duplicate = wrapper.find('a[children="Duplicate"]');
    WidgetActions.duplicate = jest.fn(() => Promise.resolve({ id: 'duplicatedWidgetId' }));
    TitlesActions.set = jest.fn((type, id, title) => {
      expect(type).toEqual(TitleTypes.Widget);
      expect(id).toEqual('duplicatedWidgetId');
      expect(title).toEqual('Dummy Widget (copy)');
      done();
    });

    duplicate.simulate('click');

    expect(WidgetActions.duplicate).toHaveBeenCalled();
  });
});
