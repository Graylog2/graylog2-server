// @flow strict
import * as Immutable from 'immutable';
import { alice, bob, manager, owner } from 'fixtures/entityShareState';

import ActiveShare from 'logic/permissions/ActiveShare';

import SelectedGrantee from './SelectedGrantee';

describe('SelectedGrantee', () => {
  const aliceIsOwner = ActiveShare.builder()
    .grant('grant-alice-id')
    .role(owner.id)
    .grantee(alice.id)
    .build();
  const activeShares = Immutable.List([aliceIsOwner]);

  const checkCurrentState = ({ grantee, role, expectedReturn }) => {
    const selectedGrantee = SelectedGrantee.create(grantee.id, grantee.title, grantee.type, role.id);
    const state = selectedGrantee.currentState(activeShares);

    expect(state).toBe(expectedReturn);
  };

  it.each`
        grantee  | role       | expectedReturn
        ${alice} | ${owner}   | ${'unchanged'}
        ${alice} | ${manager} | ${'changed'}
        ${bob}   | ${manager} | ${'new'}
  `('should return current state of $expectedReturn grantee', checkCurrentState);
});
