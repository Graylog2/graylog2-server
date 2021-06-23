/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount, shallow } from 'wrappedEnzyme';
import { alice } from 'fixtures/users';
import mockComponent from 'helpers/mocking/MockComponent';

import CurrentUserContext from 'contexts/CurrentUserContext';

import IfPermitted from './IfPermitted';

jest.mock('stores/connect', () => (x) => x);
jest.mock('injection/StoreProvider', () => ({ getStore: () => {} }));

describe('IfPermitted', () => {
  let element;

  // We can't use prop types here, they are not compatible with mount and require in this case
  // eslint-disable-next-line react/prop-types
  const SimpleIfPermitted = ({ children, currentUser, ...props }) => (
    <CurrentUserContext.Provider value={currentUser}>
      <IfPermitted {...props}>{children}</IfPermitted>
    </CurrentUserContext.Provider>
  );

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
        <SimpleIfPermitted permissions={['somepermission']}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user does not have permissions', () => {
      const currentUser = alice.toBuilder()
        .permissions(undefined)
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['somepermission']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has empty permissions', () => {
      const currentUser = alice.toBuilder().permissions(Immutable.List()).build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['somepermission']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has different permissions', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['someotherpermission']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['somepermission']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user is missing one permission', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['someotherpermission']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['somepermission', 'someotherpermission']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user is missing permission for specific id', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:otherid']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has permission for different action', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:otheraction']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has permission for id only', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });
  });

  describe('renders children if', () => {
    let wrapper;

    afterEach(() => {
      expect(wrapper).toIncludeText('Something!');
    });

    it('empty permissions were passed', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List([]))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={[]} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('empty permissions were passed and no user is present', () => {
      wrapper = mount((
        <SimpleIfPermitted permissions={[]}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('undefined permissions were passed and no user is present', () => {
      wrapper = mount((
        <SimpleIfPermitted permissions={[]}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has exact required permissions', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['something']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['something']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has any exact required permission', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['something']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['something', 'someother']} currentUser={currentUser} anyPermissions>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has exact required permission for action with entity id', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has wildcard permission', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['*']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['something']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has wildcard permission for action', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has wildcard permission for id', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:*']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has wildcard permission for entity', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
      ));
    });

    it('user has wildcard permission for entity when permission for id is required', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();

      wrapper = mount((
        <SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser}>
          {element}
        </SimpleIfPermitted>
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
