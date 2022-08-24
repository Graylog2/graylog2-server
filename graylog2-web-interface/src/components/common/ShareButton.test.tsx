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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import { createGRN } from 'logic/permissions/GRN';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import ShareButton from './ShareButton';

jest.mock('hooks/useCurrentUser');

describe('<ShareButton />', () => {
  const entityType = 'dashboard';
  const entityId = 'dashboard-id';
  const entityGRN = createGRN(entityType, entityId);
  const currentUser = adminUser.toBuilder()
    .permissions(Immutable.List([]))
    .grnPermissions(Immutable.List([`entity:own:${entityGRN}`]))
    .build();

  const SimpleShareButton = ({ onClick, ...rest }: { onClick: () => void, disabledInfo?: string | undefined }) => (
    <ShareButton {...rest} onClick={onClick} entityType={entityType} entityId={entityId} />
  );

  SimpleShareButton.defaultProps = { disabledInfo: undefined };

  it('should be clickable if user has correct permissions', async () => {
    const onClickStub = jest.fn();
    asMock(useCurrentUser).mockReturnValue(currentUser.toBuilder().grnPermissions(Immutable.List([`entity:own:${entityGRN}`])).build());
    render(<SimpleShareButton onClick={onClickStub} />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).toHaveBeenCalledTimes(1);
  });

  it('should not be clickable if user has incorrect permissions', async () => {
    const onClickStub = jest.fn();
    asMock(useCurrentUser).mockReturnValue(currentUser.toBuilder().grnPermissions(Immutable.List([])).build());
    render(<SimpleShareButton onClick={onClickStub} />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).not.toHaveBeenCalled();
  });

  it('should not be clickable if disabledInfo is provided', async () => {
    const onClickStub = jest.fn();
    asMock(useCurrentUser).mockReturnValue(currentUser.toBuilder().grnPermissions(Immutable.List([`entity:own:${entityGRN}`])).build());
    render(<SimpleShareButton onClick={onClickStub} disabledInfo="Only saved entities can be shared" />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).not.toHaveBeenCalled();
  });
});
