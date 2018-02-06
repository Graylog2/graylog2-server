import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';

import TokenList from 'components/users/TokenList';

/* https://github.com/facebook/react/issues/7371
 *
 * findDomNode with refs is not supported by the react-test-renderer.
 * So we need to mock the findDOMNode function for TableList respectievly
 * for its child component TypeAheadDataFilter.
 */
jest.mock('react-dom', () => ({
  findDOMNode: () => ({}),
}));

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
      create={createFn}
      delete={deleteFn}
    />);
    wrapper.find('#create-token-input').simulate('change', { target: { value: 'hans' } });
    wrapper.find('button[type="submit"]').at(0).simulate('click');
    expect(createFn.mock.calls.length).toBe(1);

    wrapper.find('button[children="Delete"]').at(0).simulate('click');
    expect(createFn.mock.calls.length).toBe(1);
  });

  it('should display tokens if "Hide tokens" was unchecked', () => {
    const wrapper = mount(<TokenList
      tokens={tokens}
    />);
    expect(wrapper.find('span[children="beef2001"]').length).toEqual(0);
    wrapper.find('#hide-tokens').at(0).simulate('change', { target: { checked: false } });
    expect(wrapper.find('span[children="beef2001"]').length).toEqual(1);
  });
});
