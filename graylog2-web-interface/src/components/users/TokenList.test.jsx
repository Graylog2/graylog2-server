import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import TokenList from 'components/users/TokenList';

jest.mock('components/common/ClipboardButton', () => 'clipboard-button');

describe('<TokenList />', () => {
  const tokens = [
    { name: 'Acme', token: 'beef2001' },
    { name: 'Hamfred', token: 'beef2002' },
  ];

  it('should render with empty tokens', () => {
    const wrapper = renderer.create(<TokenList tokens={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with tokens', () => {
    const wrapper = renderer.create(<TokenList tokens={tokens} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should add new token and delete existing ones', () => {
    const createFn = jest.fn((tokenName) => {
      expect(tokenName).toEqual('hans');
    });
    const deleteFn = jest.fn((token) => {
      expect(token).toEqual('beef2001');
    });
    const wrapper = mount(<TokenList
      tokens={tokens}
      onCreate={createFn}
      onDelete={deleteFn}
    />);
    wrapper.find('input#create-token-input').simulate('change', { target: { value: 'hans' } });
    wrapper.find('form').at(0).simulate('submit');
    expect(createFn.mock.calls.length).toBe(1);

    wrapper.find('button[children="Delete"]').at(0).simulate('click');
    expect(createFn.mock.calls.length).toBe(1);
  });

  it('should display tokens if "Hide tokens" was unchecked', () => {
    const wrapper = mount(<TokenList
      tokens={tokens}
    />);
    expect(wrapper.find('span[children="beef2001"]').length).toEqual(0);
    wrapper.find('input#hide-tokens').simulate('change', { target: { checked: false } });
    expect(wrapper.find('span[children="beef2001"]').length).toEqual(1);
  });
});
