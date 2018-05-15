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
    const wrapper = renderer.create(<ContentPackParameters contentPack={contentPack} appliedParameter={{}} />);
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
    const wrapper = mount(<ContentPackParameters contentPack={contentPack} onStateChange={changeFn} appliedParameter={{}} />);
    wrapper.find('button[children="Delete"]').simulate('click');
    expect(changeFn.mock.calls.length).toBe(1);
  });


});
