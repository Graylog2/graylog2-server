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
import * as Immutable from 'immutable';
import { alice, bob, manager, owner } from 'fixtures/entityShareState';

import ActiveShare from 'logic/permissions/ActiveShare';

import SelectedGrantee from './SelectedGrantee';

describe('SelectedGrantee', () => {
  const aliceIsOwner = ActiveShare.builder()
    .grant('grant-alice-id')
    .capability(owner.id)
    .grantee(alice.id)
    .build();
  const activeShares = Immutable.List([aliceIsOwner]);

  const checkCurrentState = ({ grantee, capability, expectedReturn }) => {
    const selectedGrantee = SelectedGrantee.create(grantee.id, grantee.title, grantee.type, capability.id);
    const state = selectedGrantee.currentState(activeShares);

    expect(state).toBe(expectedReturn);
  };

  it.each`
        grantee  | capability | expectedReturn
        ${alice} | ${owner}   | ${'unchanged'}
        ${alice} | ${manager} | ${'changed'}
        ${bob}   | ${manager} | ${'new'}
  `('should return current state of $expectedReturn grantee', checkCurrentState);
});
