// @flow strict
import * as Immutable from 'immutable';

import EntityShareState from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Role from 'logic/permissions/Role';
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

const availableGrantees = Immutable.List([everyone, john, jane, security]); // keep this order

// roles
export const viewer = Role.builder().id('viewer-id').title('Viewer').build();
export const manager = Role.builder().id('manager-id').title('Manager').build();
export const owner = Role.builder().id('owner-id').title('Owner').build();

const availableRoles = Immutable.List([viewer, manager, owner]);

// active shares
const janeIsOwner = ActiveShare
  .builder()
  .grant('grant-id')
  .grantee(jane.id)
  .role(owner.id)
  .build();
const activeShares = Immutable.List([janeIsOwner]);

// selected grantee roles
const janeIsSelected = Immutable.Map({ [janeIsOwner.grantee]: janeIsOwner.role });

// missing dependencies
const missingDependecy = MissingDependency
  .builder()
  .id('grn::::stream:57bc9188e62a2373778d9e03')
  .type('stream')
  .title('Security Data')
  .owners(Immutable.List([john, security]))
  .build();

export const missingDependencies = Immutable.Map({ [jane.id]: Immutable.List([missingDependecy]) });

const entityShareState = EntityShareState
  .builder()
  .entity('grn::::dashboard:dashboard-id')
  .availableGrantees(availableGrantees)
  .availableRoles(availableRoles)
  .activeShares(activeShares)
  .missingDependencies(missingDependencies)
  .selectedGranteeRoles(janeIsSelected)
  .build();

export default entityShareState;
