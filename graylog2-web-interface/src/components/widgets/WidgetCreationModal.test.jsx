import React from 'react';
import { mount } from 'wrappedEnzyme';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import WidgetCreationModal from './WidgetCreationModal';

/* eslint react/prop-types: "off", react/no-multi-comp: "off" */
const CustomModalStaticConfig = class extends React.Component {
  static initialConfiguration = {
    foo: '1',
  };

  render() {
    return null;
  }
};

const CustomModalDynamicConfig = class extends React.Component {
  constructor(props) {
    super(props);
    props.setInitialConfiguration({
      foo: '2',
    });
  }

  render() {
    return null;
  }
};

const CustomModalBothConfigs = class extends React.Component {
  static initialConfiguration = {
    foo: '3',
  };

  constructor(props) {
    super(props);
    props.setInitialConfiguration({
      foo: '4',
    });
  }

  render() {
    return null;
  }
};

PluginStore.register(new PluginManifest({}, {
  widgets: [
    {
      type: 'STATIC',
      configurationCreateComponent: CustomModalStaticConfig,
    },
    {
      type: 'DYNAMIC',
      configurationCreateComponent: CustomModalDynamicConfig,
    },
    {
      type: 'BOTH',
      configurationCreateComponent: CustomModalBothConfigs,
    },
  ],
}));

describe('<WidgetCreationModal />', () => {
  describe('with static initialConfiguration', () => {
    it('should set initial configuration', () => {
      const wrapper = mount(<WidgetCreationModal onConfigurationSaved={() => {}} widgetType="STATIC" />);
      expect(wrapper.state('config').foo).toEqual('1');
      wrapper.instance().open();
      expect(wrapper.state('config').foo).toEqual('1');
    });
  });

  describe('with dynamic setInitialConfiguration', () => {
    it('should set initial configuration', () => {
      const wrapper = mount(<WidgetCreationModal onConfigurationSaved={() => {}} widgetType="DYNAMIC" />);
      expect(wrapper.state('config').foo).toEqual(undefined);
      wrapper.instance().open();
      expect(wrapper.state('config').foo).toEqual('2');
    });
  });

  describe('with both initialConfiguration and setInitialConfiguration', () => {
    it('should set initial configuration', () => {
      const wrapper = mount(<WidgetCreationModal onConfigurationSaved={() => {}} widgetType="BOTH" />);
      expect(wrapper.state('config').foo).toEqual('3');
      wrapper.instance().open();
      expect(wrapper.state('config').foo).toEqual('4');
    });
  });
});
