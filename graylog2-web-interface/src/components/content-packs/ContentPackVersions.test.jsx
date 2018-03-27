import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';
import URLUtils from 'util/URLUtils';

import ContentPackVersions from 'components/content-packs/ContentPackVersions';

describe('<ContentPackVersions />', () => {
  URLUtils.areCredentialsInURLSupported = jest.fn(() => { return false; });
  const buildPack = (rev) => {
    return {
      id: '1',
      rev: rev,
      title: 'UFW Grok Patterns',
      description: 'Grok Patterns to extract informations from UFW logfiles',
      states: ['installed', 'edited'],
      summary: 'This is a summary',
      vendor: 'graylog.com',
      url: 'www.graylog.com',
    };
  };
  const contentPack = {
    1: buildPack(1),
    2: buildPack(2),
    3: buildPack(3),
    4: buildPack(4),
  };

  it('should render with content pack versions', () => {
    const wrapper = renderer.create(<ContentPackVersions contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should fire on change when clicked on a version', () => {
    const changeFn = jest.fn((version) => {
      expect(version).toEqual('1');
    });
    const wrapper = mount(<ContentPackVersions onChange={changeFn} contentPack={contentPack} />);
    wrapper.find('input[value=1]').simulate('change', { target: { checked: true, value: '1' } });
    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should fire on delete when clicked on delete a version', () => {
    const deleteFn = jest.fn((version, revision) => {
      expect(version).toEqual('1');
      expect(revision).toEqual(1);
    });
    const wrapper = mount(<ContentPackVersions onDeletePack={deleteFn} contentPack={contentPack} />);
    wrapper.find('a[children="Remove"]').at(0).simulate('click');
    expect(deleteFn.mock.calls.length).toBe(1);
  });
});
