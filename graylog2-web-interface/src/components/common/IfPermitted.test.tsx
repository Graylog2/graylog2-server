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
import { render, screen } from 'wrappedTestingLibrary';

import { alice } from 'fixtures/users';
import type User from 'logic/users/User';
import CurrentUserContext from 'contexts/CurrentUserContext';

import IfPermitted from './IfPermitted';

jest.mock('stores/connect', () => (x) => x);

describe('IfPermitted', () => {
  type SUTProps = Partial<React.ComponentProps<typeof IfPermitted>> & {
    currentUser?: User,
  }

  const defaultChildren = <p>Something!</p>;
  const SimpleIfPermitted = ({ children = defaultChildren, currentUser, permissions, ...rest }: SUTProps) => (
    <CurrentUserContext.Provider value={currentUser}>
      <IfPermitted permissions={permissions} {...rest}>{children}</IfPermitted>
    </CurrentUserContext.Provider>
  );

  describe('renders nothing if', () => {
    const expectToNotRenderChildren = () => {
      expect(screen.queryByText('Something!')).not.toBeInTheDocument();
    };

    it('no user is present', () => {
      render(<SimpleIfPermitted permissions={['somepermission']} />);

      expectToNotRenderChildren();
    });

    it('user does not have permissions', () => {
      const currentUser = alice.toBuilder()
        .permissions(undefined)
        .build();

      render(<SimpleIfPermitted permissions={['somepermission']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });

    it('user has empty permissions', () => {
      const currentUser = alice.toBuilder().permissions(Immutable.List()).build();

      render(<SimpleIfPermitted permissions={['somepermission']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });

    it('user has different permissions', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['someotherpermission']))
        .build();

      render(<SimpleIfPermitted permissions={['somepermission']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });

    it('user is missing one permission', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['someotherpermission']))
        .build();

      render(<SimpleIfPermitted permissions={['somepermission', 'someotherpermission']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });

    it('user is missing permission for specific id', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:otherid']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });

    it('user has permission for different action', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:otheraction']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });

    it('user has permission for id only', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action']} currentUser={currentUser} />);

      expectToNotRenderChildren();
    });
  });

  describe('renders children if', () => {
    const expectToRenderChildren = () => {
      expect(screen.getByText('Something!')).toBeInTheDocument();
    };

    it('empty permissions were passed', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List([]))
        .build();

      render(<SimpleIfPermitted permissions={[]} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('empty permissions were passed and no user is present', () => {
      render(<SimpleIfPermitted permissions={[]} />);

      expectToRenderChildren();
    });

    it('undefined permissions were passed and no user is present', () => {
      render(<SimpleIfPermitted permissions={[]} />);

      expectToRenderChildren();
    });

    it('user has exact required permissions', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['something']))
        .build();

      render(<SimpleIfPermitted permissions={['something']} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('user has any exact required permission', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['something']))
        .build();

      render(<SimpleIfPermitted permissions={['something', 'someother']} currentUser={currentUser} anyPermissions />);

      expectToRenderChildren();
    });

    it('user has exact required permission for action with entity id', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['*']))
        .build();

      render(<SimpleIfPermitted permissions={['something']} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for action', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for id', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:action:*']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for entity', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action']} currentUser={currentUser} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for entity when permission for id is required', () => {
      const currentUser = alice.toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();

      render(<SimpleIfPermitted permissions={['entity:action:id']} currentUser={currentUser} />);

      expectToRenderChildren();
    });
  });

  it('passes props to children', () => {
    const Foo = jest.fn(() => <p>Something else!</p>) as React.ElementType;
    const Bar = jest.fn(() => <p>Something else!</p>) as React.ElementType;

    render((
      // @ts-ignore
      <IfPermitted permissions={[]} something={42} otherProp={{ foo: 'bar!' }}>
        <Foo />
        <Bar />
      </IfPermitted>
    ));

    expect(Foo).toHaveBeenLastCalledWith({ something: 42, otherProp: { foo: 'bar!' } }, {});
    expect(Bar).toHaveBeenLastCalledWith({ something: 42, otherProp: { foo: 'bar!' } }, {});
  });

  it('does not pass property to children if already present', () => {
    const Foo = jest.fn(() => <p>Something else!</p>) as React.ElementType;
    const Bar = jest.fn(() => <p>Something else!</p>) as React.ElementType;

    render(
      // @ts-ignore
      <IfPermitted permissions={[]} something={42} otherProp={{ foo: 'bar!' }}>
        <Foo something={23} />
        <Bar otherProp={{ hello: 'world!' }} />
      </IfPermitted>,
    );

    expect(Foo).toHaveBeenLastCalledWith({ something: 23, otherProp: { foo: 'bar!' } }, {});
    expect(Bar).toHaveBeenLastCalledWith({ something: 42, otherProp: { hello: 'world!' } }, {});
  });
});
