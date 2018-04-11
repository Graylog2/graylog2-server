import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackParameters from 'components/content-packs/ContentPackParameters';

describe('<ContentPackParameters />', () => {
  it('should render with empty parameters', () => {
    const contentPack = {
      parameters: [],
    };
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a parameter', () => {
    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        value_type: 'string',
        default_value: 'test',
      }],
    };
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should delete a parameter', () => {
    const changeFn = jest.fn((newContentPack) => {
      expect(newContentPack.parameters).toEqual([]);
    });

    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        value_type: 'string',
        default_value: 'test',
      }],
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
        value_type: 'string',
        default_value: 'test',
      }]);
    });

    const contentPack = {
      parameters: [],
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
    };
    const wrapper = mount(<ContentPackParameters contentPack={contentPack} onStateChange={changeFn} />);
    wrapper.find('input#title').simulate('change', { target: { name: 'title', value: 'title' } });
    wrapper.find('input#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#default_value').simulate('change', { target: { name: 'default_value', value: 'test' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(changeFn.mock.calls.length).toBe(0);
  });
});
