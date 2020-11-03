// @flow strict
import { readFileSync } from 'fs';

import * as Immutable from 'immutable';
import { dirname } from 'path';
import entityShareStateFixture, { alice, bob, john, jane, everyone, security, viewer, owner, manager } from 'fixtures/entityShareState';

import ActiveShare from 'logic/permissions/ActiveShare';

import EntityShareState from './EntityShareState';

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

describe('EntityShareState', () => {
  it('should import from json', () => {
    const entityShareState = EntityShareState.fromJSON(readFixture('EntityShareState.fixtures.json'));

    expect(entityShareState.availableGrantees.size).not.toBe(undefined);
    expect(entityShareState.availableCapabilities.size).not.toBe(undefined);
    expect(entityShareState.activeShares.size).not.toBe(undefined);
    expect(entityShareState.selectedGranteeCapabilities.size).not.toBe(undefined);
    expect(entityShareState.missingDependencies.size).not.toBe(undefined);

    expect(entityShareState).toMatchSnapshot();
  });

  describe('order of selected grantees', () => {
    it('should order imported grantees', () => {
      const { selectedGrantees } = EntityShareState.fromJSON(readFixture('EntityShareState.fixtures.json'));
      const [securityImport, janeImport] = selectedGrantees.toArray();

      expect(securityImport.title).toBe('Security Folks');
      expect(janeImport.title).toBe('Jane Doe');
    });

    it('should order by type', () => {
      const janeIsOwner = ActiveShare
        .builder()
        .grant('grant-jane-id')
        .grantee(jane.id)
        .capability(owner.id)
        .build();
      const everyoneIsViewer = ActiveShare
        .builder()
        .grant('grant-everyone-id')
        .grantee(everyone.id)
        .capability(viewer.id)
        .build();
      const securityIsManager = ActiveShare
        .builder()
        .grant('grant-security-id')
        .grantee(security.id)
        .capability(manager.id)
        .build();
      const activeShares = Immutable.List([janeIsOwner, everyoneIsViewer, securityIsManager]);

      const selection = Immutable.Map({
        [janeIsOwner.grantee]: janeIsOwner.capability,
        [everyoneIsViewer.grantee]: everyoneIsViewer.capability,
        [securityIsManager.grantee]: securityIsManager.capability,
      });

      const { selectedGrantees } = entityShareStateFixture.toBuilder()
        .activeShares(activeShares)
        .selectedGranteeCapabilities(selection)
        .build();

      const [first, second, third] = selectedGrantees.toArray();

      expect(first.title).toBe('Everyone');
      expect(second.title).toBe('Security Team');
      expect(third.title).toBe('Jane Doe');
    });

    it('should order alphabetically', () => {
      const janeIsOwner = ActiveShare
        .builder()
        .grant('grant-jane-id')
        .grantee(jane.id)
        .capability(owner.id)
        .build();
      const aliceIsOwner = ActiveShare
        .builder()
        .grant('grant-jane-id')
        .grantee(alice.id)
        .capability(owner.id)
        .build();
      const bobIsViewer = ActiveShare
        .builder()
        .grant('grant-bob-id')
        .grantee(bob.id)
        .capability(viewer.id)
        .build();
      const johnIsManager = ActiveShare
        .builder()
        .grant('grant-john-id')
        .grantee(john.id)
        .capability(manager.id)
        .build();
      const activeShares = Immutable.List([janeIsOwner, aliceIsOwner, bobIsViewer, johnIsManager]);

      const selection = Immutable.Map({
        [janeIsOwner.grantee]: janeIsOwner.capability,
        [aliceIsOwner.grantee]: aliceIsOwner.capability,
        [bobIsViewer.grantee]: bobIsViewer.capability,
        [johnIsManager.grantee]: johnIsManager.capability,
      });

      const { selectedGrantees } = entityShareStateFixture.toBuilder()
        .activeShares(activeShares)
        .selectedGranteeCapabilities(selection)
        .build();

      const [first, second, third, forth] = selectedGrantees.toArray();

      expect(first.title).toBe('Alice Muad\'Dib');
      expect(second.title).toBe('Bob Bobson');
      expect(third.title).toBe('Jane Doe');
      expect(forth.title).toBe('John Wick');
    });
  });
});
