import React from 'react';
import { mount } from 'theme/enzymeWithTheme';
import 'helpers/mocking/react-dom_mock';

import ContentPackEditParameter from 'components/content-packs/ContentPackEditParameter';

describe('<ContentPackEditParameters />', () => {
  it('should render with empty parameters', () => {
    const wrapper = mount(<ContentPackEditParameter />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render a form for creation', () => {
    const parameters = [{
      name: 'A parameter name',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const wrapper = mount(<ContentPackEditParameter parameters={parameters} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render a form for edition', () => {
    const parameters = [{
      name: 'A parameter name',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];

    const parameterToEdit = {
      name: 'A parameter name',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    };
    const wrapper = mount(<ContentPackEditParameter parameters={parameters}
                                                    parameterToEdit={parameterToEdit} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should create a parameter', () => {
    const changeFn = jest.fn((newParameter) => {
      expect(newParameter).toEqual({
        name: 'name',
        title: 'title',
        description: 'descr',
        type: 'integer',
        default_value: 1,
      });
    });

    const wrapper = mount(<ContentPackEditParameter onUpdateParameter={changeFn} />);
    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('select#type').simulate('change', { target: { name: 'type', value: 'integer' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: '1' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should not create a parameter if name is missing', () => {
    const changeFn = jest.fn();

    const wrapper = mount(<ContentPackEditParameter onUpdateParameter={changeFn} />);
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(0);
  });

  it('should validate with existing names when editing a parameter', () => {
    const parameters = [
      { name: 'hans', title: 'hans', description: 'hans' },
      { name: 'franz', title: 'franz', description: 'franz' },
    ];

    const wrapper = mount(<ContentPackEditParameter parameters={parameters} parameterToEdit={parameters[0]} />);
    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'franz' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(wrapper.find('span.help-block').at(1).text()).toEqual('The parameter name must be unique.');
  });

  describe('validation', () => {
    let wrapper;

    beforeEach(() => {
      const parameters = [{ name: 'hans', title: 'hans', description: 'hans' }];

      wrapper = mount(<ContentPackEditParameter parameters={parameters} />);
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
