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

declare module 'graylog-web-plugin/plugin' {
  interface EntityActions {
    entity: 'action' | 'otheraction';
    someentity: 'read';
    someother: 'read';
    someotherentity: 'read';
    something: 'read';
  }
}

describe('IfPermitted', () => {
  type SUTProps = Partial<React.ComponentProps<typeof IfPermitted>>;

  const defaultChildren = <p>Something!</p>;
  const SimpleIfPermitted = ({ children = defaultChildren, permissions, ...rest }: SUTProps) => (
    <IfPermitted permissions={permissions} {...rest}>
      {children}
    </IfPermitted>
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
      render(<SimpleIfPermitted permissions={['someentity:read']} />);

      expectToNotRenderChildren();
    });

    it('user does not have permissions', () => {
      const user = adminUser.toBuilder().permissions(undefined).build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['someentity:read']} />);

      expectToNotRenderChildren();
    });

    it('user has empty permissions', () => {
      const user = adminUser.toBuilder().permissions(Immutable.List()).build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['someentity:read']} />);

      expectToNotRenderChildren();
    });

    it('user has different permissions', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['someotherentity:read']))
        .build();

      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['someentity:read']} />);

      expectToNotRenderChildren();
    });

    it('user is missing one permission', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['someotherentity:read']))
        .build();

      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['someentity:read', 'someotherentity:read']} />);

      expectToNotRenderChildren();
    });

    it('user is missing permission for specific id', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:action:otherid']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToNotRenderChildren();
    });

    it('user has permission for different action', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:otheraction']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action']} />);

      expectToNotRenderChildren();
    });

    it('user has permission for id only', () => {
      const user = adminUser
        .toBuilder()
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
      const user = adminUser.toBuilder().permissions(Immutable.List([])).build();
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
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['something:read']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['something:read']} />);

      expectToRenderChildren();
    });

    it('user has any exact required permission', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['something:read']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['something:read', 'someother:read']} anyPermissions />);

      expectToRenderChildren();
    });

    it('user has exact required permission for action with entity id', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:action:id']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['something:read']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for action', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:action']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for id', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:action:*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for entity', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action']} />);

      expectToRenderChildren();
    });

    it('user has wildcard permission for entity when permission for id is required', () => {
      const user = adminUser
        .toBuilder()
        .permissions(Immutable.List(['entity:*']))
        .build();
      asMock(useCurrentUser).mockReturnValue(user);
      render(<SimpleIfPermitted permissions={['entity:action:id']} />);

      expectToRenderChildren();
    });
  });
});
