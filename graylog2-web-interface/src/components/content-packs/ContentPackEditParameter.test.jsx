import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackAddParameter from 'components/content-packs/ContentPackEditParameter';

describe('<ContentPackAddParameters />', () => {
  it('should render with empty parameters', () => {
    const wrapper = renderer.create(<ContentPackAddParameter />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a parameter', () => {
    const parameters = [{
      name: 'A parameter name',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const wrapper = renderer.create(<ContentPackAddParameter parameters={parameters} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should create a parameter', () => {
    const changeFn = jest.fn((newParameter) => {
      expect(newParameter).toEqual({
        name: 'name',
        title: 'title',
        description: 'descr',
        type: 'string',
        default_value: 'test',
      });
    });

    const wrapper = mount(<ContentPackAddParameter onUpdateParameter={changeFn} />);
    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should not create a parameter if name is missing', () => {
    const changeFn = jest.fn();

    const wrapper = mount(<ContentPackAddParameter onUpdateParameter={changeFn} />);
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(0);
  });

  describe('validation', () => {
    let wrapper;

    beforeEach(() => {
      const parameters = [{ name: 'hans', title: 'hans', description: 'hans' }];

      wrapper = mount(<ContentPackAddParameter parameters={parameters} />);
      wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
      wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
      wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    });

    afterEach(() => {
      wrapper = undefined;
    });

    it('should validate the parameter name', () => {
      wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'hans' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(1).text()).toEqual('The parameter name must be unique.');

      wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'hans-dampf' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(1).text()).toEqual('The parameter name must only contain A-Z, a-z, 0-9 and _');

      wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'dampf' } });
      wrapper.find('form').at(0).simulate('submit');
      expect(wrapper.find('span.help-block').at(1).text()).toEqual('This is used as the parameter reference and must not contain a space.');
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
