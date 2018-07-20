import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';
import URLUtils from 'util/URLUtils';

import ContentPacksList from 'components/content-packs/ContentPacksList';

describe('<ContentPacksList />', () => {
  URLUtils.areCredentialsInURLSupported = jest.fn(() => { return false; });

  const contentPacks = [
    { id: '1', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0', states: ['installed', 'edited'] },
    { id: '2', title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1', states: [] },
    { id: '3', title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'] },
    { id: '4', title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4', states: ['error'] },
    { id: '5', title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0', states: ['installed', 'updatable'] },
    { id: '6', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0', states: ['installed', 'edited'] },
    { id: '7', title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1', states: [] },
    { id: '8', title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'] },
    { id: '9', title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4', states: ['error'] },
    { id: '10', title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0', states: ['installed', 'updatable'] },
    { id: '11', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0', states: ['installed', 'edited'] },
    { id: '12', title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1', states: [] },
    { id: '13', title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'] },
    { id: '14', title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4', states: ['error'] },
    { id: '15', title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0', states: ['installed', 'updatable'] },
  ];

  it('should render with empty content packs', () => {
    const wrapper = renderer.create(<ContentPacksList contentPacks={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with content packs', () => {
    const wrapper = renderer.create(<ContentPacksList contentPacks={contentPacks} />);
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
    wrapper.find('a[children="Remove all"]').at(0).simulate('click');
    expect(deleteFn.mock.calls.length).toBe(1);
  });
});
