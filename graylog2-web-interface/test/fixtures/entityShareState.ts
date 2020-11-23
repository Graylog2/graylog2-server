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
// @flow strict
import * as Immutable from 'immutable';

import EntityShareState, { MissingDependencies } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Capability from 'logic/permissions/Capability';
import SharedEntity from 'logic/permissions/SharedEntity';
import ActiveShare from 'logic/permissions/ActiveShare';
import ValidationResult from 'logic/permissions/ValidationResult';

// grantees
export const everyone = Grantee
  .builder()
  .id('grn::::user:everyone-id')
  .title('Everyone')
  .type('global')
  .build();

export const security = Grantee
  .builder()
  .id('grn::::team:security-team-id')
  .title('Security Team')
  .type('team')
  .build();

export const alice = Grantee
  .builder()
  .id('grn::::user:alice-id')
  .title('Alice Muad\'Dib')
  .type('user')
  .build();

export const bob = Grantee
  .builder()
  .id('grn::::user:bob-id')
  .title('Bob Bobson')
  .type('user')
  .build();

export const john = Grantee
  .builder()
  .id('grn::::user:john-id')
  .title('John Wick')
  .type('user')
  .build();

export const jane = Grantee
  .builder()
  .id('grn::::user:jane-id')
  .title('Jane Doe')
  .type('user')
  .build();

export const availableGrantees = Immutable.List<Grantee>([everyone, alice, bob, john, jane, security]); // keep this order

// capabilities
export const viewer = Capability.builder().id('view').title('Viewer').build();
export const manager = Capability.builder().id('manage').title('Manager').build();
export const owner = Capability.builder().id('own').title('Owner').build();

export const availableCapabilities = Immutable.List<Capability>([viewer, manager, owner]);

// active shares
const janeIsOwner = ActiveShare
  .builder()
  .grant('grant-id')
  .grantee(jane.id)
  .capability(owner.id)
  .build();
const activeShares = Immutable.List([janeIsOwner]);

// selected grantee capabilities
const janeIsSelected = Immutable.Map({ [janeIsOwner.grantee]: janeIsOwner.capability });

// missing dependencies
const missingDependecy = SharedEntity
  .builder()
  .id('grn::::stream:57bc9188e62a2373778d9e03')
  .type('stream')
  .title('Security Data')
  .owners(Immutable.List([john]))
  .build();

export const missingDependencies: MissingDependencies = Immutable.Map({ [security.id]: Immutable.List([missingDependecy]) });

const validationResult = ValidationResult.builder()
  .errorContext({
    selectedGranteeCapabilities: Immutable.List([alice.id]),
  })
  .errors({
    selectedGranteeCapabilities: Immutable.List(['An error occurred']),
  })
  .failed(true)
  .build();

export const failedEntityShareState = EntityShareState
  .builder()
  .entity('grn::::dashboard:dashboard-id')
  .availableGrantees(availableGrantees)
  .availableCapabilities(availableCapabilities)
  .activeShares(activeShares)
  .validationResults(validationResult)
  .selectedGranteeCapabilities(janeIsSelected)
  .build();

const entityShareState = EntityShareState
  .builder()
  .entity('grn::::dashboard:dashboard-id')
  .availableGrantees(availableGrantees)
  .availableCapabilities(availableCapabilities)
  .activeShares(activeShares)
  .missingDependencies(missingDependencies)
  .selectedGranteeCapabilities(janeIsSelected)
  .build();

export default entityShareState;
