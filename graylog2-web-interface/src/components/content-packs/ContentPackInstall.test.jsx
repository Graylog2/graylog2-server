import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackInstall from 'components/content-packs/ContentPackInstall';

describe('<ContentPackInstall />', () => {
  const parameter = {
    title: 'A parameter',
    name: 'PARAM',
    type: 'string',
    default_value: 'parameter',
  };

  const entity = {
    data: {
      title: { type: 'string', value: 'franz' },
      descr: { type: 'string', value: 'hans' },
    },
  };

  const contentPack = {
    id: '1',
    rev: 2,
    title: 'UFW Grok Patterns',
    description: 'Grok Patterns to extract informations from UFW logfiles',
    version: '1.0',
    states: ['installed', 'edited'],
    summary: 'This is a summary',
    vendor: 'graylog.com',
    url: 'www.graylog.com',
    parameters: [parameter],
    entities: [entity],
  };

  it('should render a install', () => {
    const wrapper = renderer.create(<ContentPackInstall contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should call install when called', () => {
    const installFn = jest.fn((id, rev, param) => {
      expect(id).toBe('1');
      expect(rev).toBe(2);
      expect(param).toEqual({ comment: 'Test', parameters: { PARAM: { type: 'string', value: 'parameter' } } });
    });

    const wrapper = mount(<ContentPackInstall contentPack={contentPack} onInstall={installFn} />);
    wrapper.find('input#comment').simulate('change', { target: { value: 'Test' } });
    wrapper.instance().onInstall();
    expect(installFn.mock.calls.length).toBe(1);
  });

  it('should not call install when parameter is missing', () => {
    const installFn = jest.fn();

    const wrapper = mount(<ContentPackInstall contentPack={contentPack} onInstall={installFn} />);
    wrapper.find('input').at(1).simulate('change', { target: { value: '' } });
    wrapper.instance().onInstall();
    expect(installFn.mock.calls.length).toBe(0);
  });
});
