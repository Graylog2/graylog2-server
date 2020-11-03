// @flow strict
import React from 'react';
import { render, screen, fireEvent } from 'wrappedTestingLibrary';
import { viewsManager } from 'fixtures/users';

import { createGRN } from 'logic/permissions/GRN';
import CurrentUserContext from 'contexts/CurrentUserContext';

import ShareButton from './ShareButton';

const entityType = 'dashboard';
const entityId = 'dashboard-id';
const entityGRN = createGRN(entityType, entityId);
const SimpleShareButton = ({ onClick, grnPermissions, ...rest }: { onClick: () => void, grnPermissions: Array<string> }) => (
  <CurrentUserContext.Provider value={{ ...viewsManager, grn_permissions: grnPermissions }}>
    <ShareButton {...rest} onClick={onClick} entityType={entityType} entityId={entityId} />
  </CurrentUserContext.Provider>
);

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

  it('should not be clickable if button is disabled', async () => {
    const onClickStub = jest.fn();
    render(<SimpleShareButton onClick={onClickStub} grnPermissions={[`entity:own:${entityGRN}`]} disabled />);

    const button = screen.getByRole('button', { name: /Share/ });
    fireEvent.click(button);

    expect(onClickStub).not.toHaveBeenCalled();
  });
});
