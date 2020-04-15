import React from 'react';
import { mount, shallow } from 'wrappedEnzyme';
import mockComponent from 'helpers/mocking/MockComponent';

import IfPermitted from './IfPermitted';

jest.mock('stores/connect', () => (x) => x);
jest.mock('injection/StoreProvider', () => ({ getStore: () => {} }));

describe('IfPermitted', () => {
  let element;
  beforeEach(() => {
    element = <p>Something!</p>;
  });
  describe('renders nothing if', () => {
    let wrapper;
    afterEach(() => {
      expect(wrapper.find('IfPermitted')).toBeEmptyRender();
    });
    it('no user is present', () => {
      wrapper = mount((
        <IfPermitted permissions={['somepermission']}>
          {element}
        </IfPermitted>
      ));
    });
    it('user does not have permissions', () => {
      wrapper = mount((
        <IfPermitted permissions={['somepermission']} currentUser={{}}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has empty permissions', () => {
      wrapper = mount((
        <IfPermitted permissions={['somepermission']} currentUser={{ permissions: [] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has undefined permissions', () => {
      wrapper = mount((
        <IfPermitted permissions={['somepermission']} currentUser={{ permissions: undefined }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has different permissions', () => {
      wrapper = mount((
        <IfPermitted permissions={['somepermission']} currentUser={{ permissions: ['someotherpermission'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user is missing one permission', () => {
      wrapper = mount((
        <IfPermitted permissions={['somepermission', 'someotherpermission']} currentUser={{ permissions: ['someotherpermission'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user is missing permission for specific id', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action:id']} currentUser={{ permissions: ['entity:action:otherid'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has permission for different action', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action']} currentUser={{ permissions: ['entity:otheraction'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has permission for id only', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action']} currentUser={{ permissions: ['entity:action:id'] }}>
          {element}
        </IfPermitted>
      ));
    });
  });
  describe('renders children if', () => {
    let wrapper;
    afterEach(() => {
      expect(wrapper).toIncludeText('Something!');
    });
    it('empty permissions were passed', () => {
      wrapper = mount((
        <IfPermitted permissions={[]} currentUser={{ permissions: [] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('empty permissions were passed and no user is present', () => {
      wrapper = mount((
        <IfPermitted permissions={[]}>
          {element}
        </IfPermitted>
      ));
    });
    it('undefined permissions were passed and no user is present', () => {
      wrapper = mount((
        <IfPermitted permissions={[]}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has exact required permissions', () => {
      wrapper = mount((
        <IfPermitted permissions={['something']} currentUser={{ permissions: ['something'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has any exact required permission', () => {
      wrapper = mount((
        <IfPermitted permissions={['something', 'someother']} currentUser={{ permissions: ['something'] }} anyPermissions>
          {element}
        </IfPermitted>
      ));
    });
    it('user has exact required permission for action with entity id', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action:id']} currentUser={{ permissions: ['entity:action:id'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has wildcard permission', () => {
      wrapper = mount((
        <IfPermitted permissions={['something']} currentUser={{ permissions: ['*'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has wildcard permission for action', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action:id']} currentUser={{ permissions: ['entity:action'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has wildcard permission for id', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action:id']} currentUser={{ permissions: ['entity:action:*'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has wildcard permission for entity', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action']} currentUser={{ permissions: ['entity:*'] }}>
          {element}
        </IfPermitted>
      ));
    });
    it('user has wildcard permission for entity when permission for id is required', () => {
      wrapper = mount((
        <IfPermitted permissions={['entity:action:id']} currentUser={{ permissions: ['entity:*'] }}>
          {element}
        </IfPermitted>
      ));
    });
  });
  it('passes props to children', () => {
    const Foo = mockComponent('Foo');
    const Bar = mockComponent('Bar');
    const wrapper = shallow((
      <IfPermitted permissions={[]} something={42} otherProp={{ foo: 'bar!' }}>
        <Foo />
        <Bar />
      </IfPermitted>
    ));

    expect(wrapper.find(Foo)).toHaveProp('something', 42);
    expect(wrapper.find(Foo)).toHaveProp('otherProp', { foo: 'bar!' });
    expect(wrapper.find(Bar)).toHaveProp('something', 42);
    expect(wrapper.find(Bar)).toHaveProp('otherProp', { foo: 'bar!' });
  });
  it('does not pass property to children if already present', () => {
    const Foo = mockComponent('Foo');
    const Bar = mockComponent('Bar');
    const wrapper = shallow((
      <IfPermitted permissions={[]} something={42} otherProp={{ foo: 'bar!' }}>
        <Foo something={23} />
        <Bar otherProp={{ hello: 'world!' }} />
      </IfPermitted>
    ));

    expect(wrapper.find(Foo)).toHaveProp('something', 23);
    expect(wrapper.find(Foo)).toHaveProp('otherProp', { foo: 'bar!' });
    expect(wrapper.find(Bar)).toHaveProp('something', 42);
    expect(wrapper.find(Bar)).toHaveProp('otherProp', { hello: 'world!' });
  });
});
