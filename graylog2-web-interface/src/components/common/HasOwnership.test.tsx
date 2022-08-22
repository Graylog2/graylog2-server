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

import { alice as currentUser, adminUser } from 'fixtures/users';
import { createGRN } from 'logic/permissions/GRN';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';

import HasOwnership from './HasOwnership';

jest.mock('hooks/useCurrentUser');

describe('HasOwnership', () => {
  const type = 'stream';
  const id = '000000000001';
  const grn = createGRN(type, id);
  const grnPermission = `entity:own:${grn}`;

  const otherType = 'dashboard';
  const otherId = 'beef000011';
  const otherGrn = createGRN(otherType, otherId);
  const otherGrnPermission = `entity:own:${otherGrn}`;

  // eslint-disable-next-line react/prop-types
  const DisabledComponent = ({ disabled }: { disabled: boolean}) => {
    return disabled
      ? <span>disabled</span>
      : <span>enabled</span>;
  };

  type Props = {
    id: string,
    type: string,
    hideChildren?: boolean,
  };

  const SimpleHasOwnership = (props: Props) => (
    <HasOwnership {...props}>
      {({ disabled }) => (
        <DisabledComponent disabled={disabled} />
      )}
    </HasOwnership>
  );

  SimpleHasOwnership.defaultProps = {
    hideChildren: false,
  };

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('should render children enabled if user has ownership', () => {
    const user = adminUser.toBuilder()
      .grnPermissions(Immutable.List([grnPermission]))
      .permissions(Immutable.List())
      .build();

    asMock(useCurrentUser).mockReturnValue(user);

    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('enabled')).toBeTruthy();
  });

  it('should render children disabled if user has empty ownership and is not admin', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List(Immutable.List()))
      .permissions(Immutable.List())
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('disabled')).toBeTruthy();
  });

  it('should render children disabled if user has wrong ownership and is not admin', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List([otherGrnPermission]))
      .permissions(Immutable.List())
      .build();

    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('disabled')).toBeTruthy();
  });

  it('should render children disabled if user has wrong ownership and is reader', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List([otherGrnPermission]))
      .permissions(Immutable.List([`streams:read:${id}`]))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('disabled')).toBeTruthy();
  });

  it('should render children disabled if user has no ownership and is reader', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List([]))
      .permissions(Immutable.List([`streams:read:${id}`]))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('disabled')).toBeTruthy();
  });

  it('should render children enabled if user has empty ownership and is admin', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List([]))
      .permissions(Immutable.List(['*']))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('enabled')).toBeTruthy();
  });

  it('should render children enabled if user has wrong ownership and is admin', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List([otherGrnPermission]))
      .permissions(Immutable.List(['*']))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} />);

    expect(screen.getByText('enabled')).toBeTruthy();
  });

  it('should hide children when configured', () => {
    const user = currentUser.toBuilder()
      .grnPermissions(Immutable.List([otherGrnPermission]))
      .permissions(Immutable.List([]))
      .build();
    asMock(useCurrentUser).mockReturnValue(user);
    render(<SimpleHasOwnership id={id} type={type} hideChildren />);

    expect(screen.queryByText('disabled')).not.toBeInTheDocument();
    expect(screen.queryByText('enabled')).not.toBeInTheDocument();
  });
});
