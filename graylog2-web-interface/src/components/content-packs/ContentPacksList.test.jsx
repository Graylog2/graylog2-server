import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';
import URLUtils from 'util/URLUtils';

import ContentPacksList from 'components/content-packs/ContentPacksList';

describe('<ContentPacksList />', () => {
  URLUtils.areCredentialsInURLSupported = jest.fn(() => { return false; });

  const contentPacks = [
    { id: '1', rev: 1, title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0' },
    { id: '2', rev: 1, title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1' },
    { id: '3', rev: 1, title: 'Backup Content Pack', summary: '', version: '3.0' },
    { id: '4', rev: 1, title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4' },
    { id: '5', rev: 1, title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0' },
    { id: '6', rev: 1, title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0' },
    { id: '7', rev: 1, title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1' },
    { id: '8', rev: 1, title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'] },
    { id: '9', rev: 1, title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4' },
    { id: '10', rev: 1, title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0' },
    { id: '11', rev: 1, title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0' },
    { id: '12', rev: 1, title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1' },
    { id: '13', rev: 1, title: 'Backup Content Pack', summary: '', version: '3.0' },
    { id: '14', rev: 1, title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4' },
    { id: '15', rev: 1, title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0' },
  ];

  it('should render with empty content packs', () => {
    const wrapper = renderer.create(<ContentPacksList contentPacks={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with content packs', () => {
    const metadata = {
      1: { 1: { installation_count: 1 } },
      2: { 5: { installation_count: 2 } },
    };
    const wrapper = renderer.create(<ContentPacksList contentPacks={contentPacks} contentPackMetadata={metadata} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should do pagination', () => {
    const wrapper = mount(<ContentPacksList contentPacks={contentPacks} />);
    const beforeFilter = wrapper.find('div.content-packs-summary').length;
    expect(beforeFilter).toBe(10);
    wrapper.find('span[children="›"]').at(0).simulate('click');
    const afterFilter = wrapper.find('div.content-packs-summary').length;
    expect(afterFilter).toBe(5);
  });

  it('should delete a content pack', () => {
    const deleteFn = jest.fn((token) => {
      expect(token).toEqual('1');
    });
    const wrapper = mount(<ContentPacksList contentPacks={contentPacks} onDeletePack={deleteFn} />);
    wrapper.find('a[children="Delete All Versions"]').at(0).simulate('click');
    expect(deleteFn.mock.calls.length).toBe(1);
  });
});
