import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackSelection from 'components/content-packs/ContentPackSelection';

describe('<ContentPackSelection />', () => {
  it('should render with empty content pack', () => {
    const contentPack = {};
    const wrapper = renderer.create(<ContentPackSelection contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with filled content pack', () => {
    const contentPack = {
      name: 'name',
      summary: 'summmary',
      description: 'descr',
      vendor: 'vendor',
      url: 'http://example.com',
    };

    const wrapper = renderer.create(<ContentPackSelection contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should update the state when filling out the form', () => {
    let called = 0;
    const changeFn = jest.fn((newContentPack) => {
      const contentPack = {
        name: 'name',
        summary: 'summary',
        description: 'descr',
        vendor: 'vendor',
        url: 'url',
      };
      called += 1;
      if (called === Object.keys(contentPack).length) {
        expect(newContentPack).toEqual(contentPack);
      }
    });
    const contentPack = {};
    const wrapper = mount(<ContentPackSelection contentPack={contentPack} onStateChange={changeFn} />);
    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
    wrapper.find('input#summary').simulate('change', { target: { name: 'summary', value: 'summary' } });
    wrapper.find('textarea#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#vendor').simulate('change', { target: { name: 'vendor', value: 'vendor' } });
    wrapper.find('input#url').simulate('change', { target: { name: 'url', value: 'url' } });
    expect(changeFn.mock.calls.length).toBe(5);
  });
});
