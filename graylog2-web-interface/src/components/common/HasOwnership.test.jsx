// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import { viewsManager } from 'fixtures/users';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { createGRN } from 'logic/permissions/GRN';

import HasOwnership from './HasOwnership';

type Props = {
  children: React.Node | { disabled: boolean } => React.Node,
  currentUser: {|
    grn_permissions: string[],
    permissions: string[],
  |},
  id: string,
  type: string,
};

describe('HasOwnership', () => {
  const SimpleHasOwnership = ({ children, currentUser, ...props }: Props) => (
    <CurrentUserContext.Provider value={{ ...viewsManager, ...currentUser }}>
      <HasOwnership {...props}>
        {children}
      </HasOwnership>
    </CurrentUserContext.Provider>
  );

  // eslint-disable-next-line react/prop-types
  const DisabledComponent = ({ disabled }: { disabled: boolean}) => {
    return disabled
      ? <span>disabled</span>
      : <span>enabled</span>;
  };

  const type = 'stream';
  const id = '000000000001';
  const grn = createGRN(id, type);
  const grnPermission = `entity:own:${grn}`;

  const otherType = 'dashboard';
  const otherId = 'beef000011';
  const otherGrn = createGRN(otherId, otherType);
  const otherGrnPermission = `entity:own:${otherGrn}`;

  it('should render children if user has ownership', () => {
    const user = { grn_permissions: [grnPermission], permissions: [] };
    const { getByText: queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeTruthy();
  });

  it('should not render children if user has empty ownership and is not admin', () => {
    const user = { grn_permissions: [], permissions: [] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeFalsy();
  });

  it('should not render children if user has wrong ownership and is not admin', () => {
    const user = { grn_permissions: [otherGrnPermission], permissions: [] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeFalsy();
  });

  it('should not render children if user has wrong ownership and is reader', () => {
    const user = { grn_permissions: [otherGrnPermission], permissions: [`streams:read:${id}`] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeFalsy();
  });

  it('should not render children if user has no ownership and is reader', () => {
    const user = { grn_permissions: [], permissions: [`streams:read:${id}`] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeFalsy();
  });

  it('should render children if user has empty ownership and is admin', () => {
    const user = { grn_permissions: [], permissions: ['*'] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeTruthy();
  });

  it('should render children if user has wrong ownership and is admin', () => {
    const user = { grn_permissions: [otherGrnPermission], permissions: ['*'] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type}>
        <span>Lovecraft</span>
      </SimpleHasOwnership>,
    );

    expect(queryByText('Lovecraft')).toBeTruthy();
  });

  it('should disable children when configured', () => {
    const user = { grn_permissions: [otherGrnPermission], permissions: [] };
    const { queryByText } = render(
      <SimpleHasOwnership currentUser={user} id={id} type={type} disableChildren>
        {({ disabled }) => (
          <DisabledComponent disabled={disabled} />
        )}
      </SimpleHasOwnership>,
    );

    expect(queryByText('disabled')).toBeTruthy();
  });
});
