import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackParameters from 'components/content-packs/ContentPackParameters';

describe('<ContentPackParameters />', () => {
  it('should render with empty parameters', () => {
    const contentPack = {
      parameters: [],
      entities: [],
    };
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a parameter', () => {
    const entity = {
      id: '111-beef',
      v: '1.0',
      data: { name: 'Input', title: 'A good input', configuration: { listen_address: '1.2.3.4', port: '23' } },
    };
    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      }],
      entities: [entity],
    };
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} appliedParameter={{}} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should delete a parameter', () => {
    const changeFn = jest.fn((newState) => {
      expect(newState.contentPack.parameters).toEqual([]);
    });

    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      }],
      entities: [],
    };
    const wrapper = mount(<ContentPackParameters contentPack={contentPack} onStateChange={changeFn} />);
    wrapper.find('button[children="Delete"]').simulate('click');
    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should create a parameter', () => {
    const changeFn = jest.fn((newState) => {
      expect(newState.contentPack.parameters).toEqual([{
        name: 'name',
        title: 'title',
        description: 'descr',
        type: 'string',
        default_value: 'test',
      }]);
    });

    const contentPack = {
      parameters: [],
      entities: [],
    };
    const wrapper = mount(<ContentPackParameters contentPack={contentPack} onStateChange={changeFn} />);
    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should not create a parameter if name is missing', () => {
    const changeFn = jest.fn();

    const contentPack = {
      parameters: [],
      entities: [],
    };
    const wrapper = mount(<ContentPackParameters contentPack={contentPack} onStateChange={changeFn} />);
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(0);
  });

  describe('validation', () => {
    let wrapper;

    beforeEach(() => {
      const contentPack = {
        parameters: [],
        entities: [],
      };

      wrapper = mount(<ContentPackParameters contentPack={contentPack} />);
      wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
      wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
      wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    });

    afterEach(() => {
      wrapper = undefined;
    });

    it('should validate the parameter input from type double', () => {
      wrapper.find('select#type').simulate('change', { target: { name: 'type', value: 'double' } });
      wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(4).text()).toEqual('This is not a double value.');
      wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: '1.0' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(4).text())
        .toEqual('Give a default value if the parameter is not optional.');
    });

    it('should validate the parameter input from type double', () => {
      wrapper.find('select#type').simulate('change', { target: { name: 'type', value: 'integer' } });
      wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(4).text()).toEqual('This is not an integer value.');
      wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: '1' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(4).text())
        .toEqual('Give a default value if the parameter is not optional.');
    });

    it('should validate the parameter input from type double', () => {
      wrapper.find('select#type').simulate('change', { target: { name: 'type', value: 'boolean' } });
      wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(4).text()).toEqual('This is not a boolean value. It must be either true or false.');
      wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'true' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(4).text())
        .toEqual('Give a default value if the parameter is not optional.');
    });
  });
});
