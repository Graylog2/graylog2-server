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
        editComponent: 'edit-dummy-visualization',
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
  const DummyWidget = props => (
    <Widget widget={widget}
            id="widgetId"
            fields={[]}
            onPositionsChange={() => {}}
            onSizeChange={() => {}}
            title="Widget Title"
            position={new WidgetPosition(1, 1, 1, 1)}
            {...props} />

  );
  it('should render with empty props', () => {
    const options = { createNodeMock };
    const wrapper = renderer.create(<DummyWidget />, options);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
  it('should render loading widget for widget without data', () => {
    const wrapper = mount(<DummyWidget />);
    expect(wrapper.find(LoadingWidget)).toHaveLength(1);
  });
  it('should render error widget for widget with one error', () => {
    const wrapper = mount(<DummyWidget errors={[{ description: 'The widget has failed: the dungeon collapsed, you die!' }]} />);
    const errorWidgets = wrapper.find(ErrorWidget);
    expect(errorWidgets).toHaveLength(1);
    expect(errorWidgets).toIncludeText('The widget has failed: the dungeon collapsed, you die!');
  });
  it('should render error widget including all error messages for widget with multiple errors', () => {
    const wrapper = mount((
      <DummyWidget errors={[
                { description: 'Something is wrong' },
                { description: 'Very wrong' },
              ]} />
    ));
    const errorWidgets = wrapper.find(ErrorWidget);
    expect(errorWidgets).toHaveLength(1);
    expect(errorWidgets).toIncludeText('Something is wrong');
    expect(errorWidgets).toIncludeText('Very wrong');
  });
  it('should render correct widget visualization for widget with data', () => {
    const wrapper = mount(<DummyWidget data={[]} />);
    expect(wrapper.find(LoadingWidget)).toHaveLength(0);
    expect(wrapper.find('dummy-visualization')).toHaveLength(1);
  });

  it('copies title when duplicating widget', (done) => {
    const wrapper = mount(<DummyWidget title="Dummy Widget" />);

    wrapper.find('ActionToggle').simulate('click');
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
  it('adds cancel action to widget in edit mode', () => {
    const wrapper = mount(<DummyWidget editing />);
    const cancel = wrapper.find('Button[children="Cancel"]');
    expect(cancel).toExist();
  });
  it('does not trigger action when clicking cancel after no changes were made', () => {
    const wrapper = mount(<DummyWidget editing />);

    WidgetActions.updateConfig = jest.fn();

    const cancelButton = wrapper.find('Button[children="Cancel"]');
    cancelButton.simulate('click');

    expect(WidgetActions.updateConfig).not.toHaveBeenCalled();
  });
  it('restores original state of widget config when clicking cancel after changes were made', () => {
    const widgetWithConfig = { config: { foo: 42 }, id: 'widgetId', type: 'dummy' };
    const wrapper = mount(<DummyWidget editing widget={widgetWithConfig} />);

    const editComponent = wrapper.find('edit-dummy-visualization').at(0);
    const widgetConfigChange = editComponent.props().onChange;

    WidgetActions.updateConfig = jest.fn();
    widgetConfigChange({ foo: 23 });
    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = wrapper.find('Button[children="Cancel"]');
    cancelButton.simulate('click');

    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 42 });
  });
  it('does not restores original state of widget config when clicking "Finish Editing"', () => {
    const widgetWithConfig = { config: { foo: 42 }, id: 'widgetId', type: 'dummy' };
    const wrapper = mount(<DummyWidget editing widget={widgetWithConfig} />);

    const editComponent = wrapper.find('edit-dummy-visualization').at(0);
    const widgetConfigChange = editComponent.props().onChange;

    WidgetActions.updateConfig = jest.fn();
    widgetConfigChange({ foo: 23 });
    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = wrapper.find('Button[children="Save"]');
    cancelButton.simulate('click');

    expect(WidgetActions.updateConfig).not.toHaveBeenCalledWith('widgetId', { foo: 42 });
  });
});
