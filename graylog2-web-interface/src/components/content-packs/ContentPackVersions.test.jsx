import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackVersions from 'components/content-packs/ContentPackVersions';

describe('<ContentPackVersions />', () => {
  const versions = ['1', '2', '3', '4'];
  it('should render with no content pack versions', () => {
    const wrapper = renderer.create(<ContentPackVersions versions={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with content pack versions', () => {
    const wrapper = renderer.create(<ContentPackVersions versions={[versions]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should fire on change when clicked on a version', () => {
    const changeFn = jest.fn((version) => {
      expect(version).toEqual('1');
    });
    const wrapper = mount(<ContentPackVersions versions={versions} onChange={changeFn} />);
    wrapper.find('input[value="1"]').simulate('change', { target: { checked: true, value: '1' } });
    expect(changeFn.mock.calls.length).toBe(1);
  });
});
