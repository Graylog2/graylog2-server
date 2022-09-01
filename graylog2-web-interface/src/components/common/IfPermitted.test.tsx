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
import { defaultUser } from 'defaultMockValues';

import { adminUser } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';

import IfPermitted from './IfPermitted';

jest.mock('stores/connect', () => (x) => x);
jest.mock('hooks/useCurrentUser');

describe('IfPermitted', () => {
  type SUTProps = Partial<React.ComponentProps<typeof IfPermitted>>;

  const defaultChildren = <p>Something!</p>;
  const SimpleIfPermitted = ({ children = defaultChildren, permissions, ...rest }: SUTProps) => (
    <IfPermitted permissions={permissions} {...rest}>{children}</IfPermitted>
  );

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  describe('renders nothing if', () => {
    const expectToNotRenderChildren = () => {
      expect(screen.queryByText('Something!')).not.toBeInTheDocument();
    };

    it('no user is present', () => {
      asMock(useCurrentUser).mockReturnValue(undefined);
      render(<SimpleIfPermitted permissions={['somepermission']} />);

      expectToNotRenderChildren();
    });

    it('user does not have permissions', () => {
      const user = adminUser.toBuilder()
        .permissions(undefined)
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['somepermission']} />);

      expectToNotRenderChildren();
    });

    it('user has empty permissions', () => {
      const user = adminUser.toBuilder().permissions(Immutable.List()).build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['somepermission']} />);

      expectToNotRenderChildren();
    });

    it('user has different permissions', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['someotherpermission']))
        .build();

      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['somepermission']} />);

      expectToNotRenderChildren();
    });

    it('user is missing one permission', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['someotherpermission']))
        .build();

      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['somepermission', 'someotherpermission']} />);

      expectToNotRenderChildren();
    });

    it('user is missing permission for specific id', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:action:otherid']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToNotRenderChildren();
    });

    it('user has permission for different action', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:otheraction']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action']} />);

      expectToNotRenderChildren();
    });

    it('user has permission for id only', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action']} />);

      expectToNotRenderChildren();
    });
  });

  describe('renders children if', () => {
    const expectToRenderChildren = () => {
      expect(screen.getByText('Something!')).toBeInTheDocument();
    };

    it('empty permissions were passed', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={[]} />);

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
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['something']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['something']} />);

      expectToRenderChildren();
    });

    it('user has any exact required permission', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['something']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['something', 'someother']} anyPermissions />);

      expectToRenderChildren();
    });

    it('user has exact required permission for action with entity id', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['something']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for action', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:action']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for id', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:action:*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for entity', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for entity when permission for id is required', () => {
      const user = adminUser.toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

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
