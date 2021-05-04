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
import React from 'react';
import { render, screen, fireEvent } from 'wrappedTestingLibrary';
import { viewsManager } from 'fixtures/users';

import { createGRN } from 'logic/permissions/GRN';
import CurrentUserContext from 'contexts/CurrentUserContext';

import ShareButton from './ShareButton';

const entityType = 'dashboard';
const entityId = 'dashboard-id';
const entityGRN = createGRN(entityType, entityId);
const SimpleShareButton = ({ onClick, grnPermissions, ...rest }: { onClick: () => void, grnPermissions: Array<string>, disabledInfo?: string | undefined }) => (
  <CurrentUserContext.Provider value={{ ...viewsManager, grn_permissions: grnPermissions }}>
    <ShareButton {...rest} onClick={onClick} entityType={entityType} entityId={entityId} />
  </CurrentUserContext.Provider>
);

SimpleShareButton.defaultProps = { disabledInfo: undefined };

describe('<ShareButton />', () => {
  it('should be clickable if user has correct permissions', async () => {
    const onClickStub = jest.fn();
    render(<SimpleShareButton onClick={onClickStub} grnPermissions={[`entity:own:${entityGRN}`]} />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).toHaveBeenCalledTimes(1);
  });

  it('should not be clickable if user has incorrect permissions', async () => {
    const onClickStub = jest.fn();
    render(<SimpleShareButton onClick={onClickStub} grnPermissions={[]} />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).not.toHaveBeenCalled();
  });

  it('should not be clickable if disabledInfo is provided', async () => {
    const onClickStub = jest.fn();
    render(<SimpleShareButton onClick={onClickStub} grnPermissions={[`entity:own:${entityGRN}`]} disabledInfo="Only saved entities can be shared" />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).not.toHaveBeenCalled();
  });
});
