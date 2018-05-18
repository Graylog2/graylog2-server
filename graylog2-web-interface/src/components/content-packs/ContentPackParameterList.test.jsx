import React from 'react';
import { mount } from 'enzyme';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';

describe('<ContentPackParameterList />', () => {
  it('should render with empty parameters with readOnly', () => {
    const contentPack = { parameters: [] };
    const wrapper = renderer.create(<ContentPackParameterList contentPack={contentPack} readOnly />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with parameters with readOnly', () => {
    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      }],
    };
    const wrapper = renderer.create(<ContentPackParameterList contentPack={contentPack} readOnly />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with empty parameters without readOnly', () => {
    const contentPack = { parameters: [] };
    const wrapper = renderer.create(<ContentPackParameterList contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with parameters without readOnly', () => {
    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      }],
    };
    const wrapper = renderer.create(<ContentPackParameterList contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should delete a parameter', () => {
    const deleteFn = jest.fn((parameter) => {
      expect(parameter.name).toEqual('A parameter name');
    });

    const contentPack = {
      parameters: [{
        name: 'A parameter name',
        title: 'A parameter title',
        description: 'A parameter descriptions',
        type: 'string',
        default_value: 'test',
      }],
    };
    const wrapper = mount(<ContentPackParameterList contentPack={contentPack}
                                                 onDeleteParameter={deleteFn} />);
    wrapper.find('button[children="Delete"]').simulate('click');
    expect(deleteFn.mock.calls.length).toBe(1);
  });
});
