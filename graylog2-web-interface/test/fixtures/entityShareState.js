// @flow strict
import * as Immutable from 'immutable';

import EntityShareState, { type MissingDependencies } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Capability from 'logic/permissions/Capability';
import MissingDependency from 'logic/permissions/MissingDependency';
import ActiveShare from 'logic/permissions/ActiveShare';

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

const availableGrantees = Immutable.List([everyone, alice, bob, john, jane, security]); // keep this order

// capabilities
export const viewer = Capability.builder().id('viewer-id').title('Viewer').build();
export const manager = Capability.builder().id('manager-id').title('Manager').build();
export const owner = Capability.builder().id('owner-id').title('Owner').build();

const availableCapabilities = Immutable.List([viewer, manager, owner]);

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
const missingDependecy = MissingDependency
  .builder()
  .id('grn::::stream:57bc9188e62a2373778d9e03')
  .type('stream')
  .title('Security Data')
  .owners(Immutable.List([john, security]))
  .build();

export const missingDependencies: MissingDependencies = Immutable.Map({ [jane.id]: Immutable.List([missingDependecy]) });

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
