import React from 'react';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPack from 'logic/content-packs/ContentPack';
import Entity from 'logic/content-packs/Entity';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';

describe('<ContentPackParameterList />', () => {
  it('should render with empty parameters with readOnly', () => {
    const contentPack = ContentPack.builder().build();
    const wrapper = mount(<ContentPackParameterList contentPack={contentPack} readOnly />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with parameters with readOnly', () => {
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();
    const wrapper = mount(<ContentPackParameterList contentPack={contentPack} readOnly />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with empty parameters without readOnly', () => {
    const contentPack = ContentPack.builder().build();
    const wrapper = mount(<ContentPackParameterList contentPack={contentPack} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with parameters without readOnly', () => {
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();
    const wrapper = mount(<ContentPackParameterList contentPack={contentPack} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should delete a parameter', () => {
    const deleteFn = jest.fn((parameter) => {
      expect(parameter.name).toEqual('PARAM');
    });
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();

    const wrapper = mount(<ContentPackParameterList contentPack={contentPack}
                                                    onDeleteParameter={deleteFn} />);
    wrapper.find('button[children="Delete"]').simulate('click');
    expect(deleteFn.mock.calls.length).toBe(1);
  });

  it('should not delete a used parameter', () => {
    const entity = Entity.builder()
      .v(1)
      .type('input')
      .id('dead-beef')
      .data({ title: { '@type': 'parameter', '@value': 'PARAM' } })
      .build();
    const deleteFn = jest.fn((parameter) => {
      expect(parameter.name).toEqual('PARAM');
    });
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const appliedParameter = {
      'dead-beef': [{ paramName: 'PARAM', configKey: 'title' }],
    };
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .entities([entity])
      .build();

    const wrapper = mount(<ContentPackParameterList contentPack={contentPack}
                                                    onDeleteParameter={deleteFn}
                                                    appliedParameter={appliedParameter} />);
    wrapper.find('button[children="Delete"]').simulate('click');
    expect(deleteFn.mock.calls.length).toBe(0);
  });

  it('should filter parameters', () => {
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }, {
      name: 'BAD_PARAM',
      title: 'real bad',
      description: 'The dark ones own parameter',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();
    const wrapper = mount(<ContentPackParameterList contentPack={contentPack} />);
    expect(wrapper.find("td[children='PARAM']").exists()).toBe(true);
    wrapper.find('input').simulate('change', { target: { value: 'Bad' } });
    wrapper.find('form').simulate('submit');
    expect(wrapper.find("td[children='PARAM']").exists()).toBe(false);
    wrapper.find("button[children='Reset']").simulate('click');
    expect(wrapper.find("td[children='PARAM']").exists()).toBe(true);
  });
});
