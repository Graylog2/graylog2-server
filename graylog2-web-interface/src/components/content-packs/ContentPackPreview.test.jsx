import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackPreview from 'components/content-packs/ContentPackPreview';

describe('<ContentPackPreview />', () => {
  it('should render with empty content pack', () => {
    const contentPack = {};
    const wrapper = renderer.create(<ContentPackPreview contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with filled content pack', () => {
    const contentPack = {
      name: 'name',
      summary: 'summmary',
      description: 'descr',
      vendor: 'vendor',
      url: 'http://example.com',
      parameters: [],
    };

    const wrapper = renderer.create(<ContentPackPreview contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should call onSave when creating a content pack', () => {
    const contentPack = {
      name: 'name',
      summary: 'summmary',
      description: 'descr',
      vendor: 'vendor',
      url: 'http://example.com',
      parameters: [],
    };

    const onSave = jest.fn();
    const wrapper = mount(<ContentPackPreview contentPack={contentPack} onSave={onSave} />);
    wrapper.find('button#create').simulate('click');
    expect(onSave.mock.calls.length).toBe(1);
  });
});
