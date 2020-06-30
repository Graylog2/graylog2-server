// @flow strict
import * as Immutable from 'immutable';

import type {
  Grantee as GranteeType,
  Role as RoleType,
  MissingDependency as MissingDependencyType,
  ActiveShare as ActiveShareType,
  GRN,
} from 'logic/permissions/types';
import { defaultCompare } from 'views/logic/DefaultCompare';

import Role from './Role';
import Grantee from './Grantee';
import ActiveShare from './ActiveShare';
import MissingDependency from './MissingDependency';
import SelectedGrantee from './SelectedGrantee';
import type { GranteeInterface } from './GranteeInterface';

export type AvailableGrantees = Immutable.List<Grantee>;
export type AvailableRoles = Immutable.List<Role>;
export type ActiveShares = Immutable.List<ActiveShare>;
export type SelectedGranteeRoles = Immutable.Map<$PropertyType<GranteeType, 'id'>, $PropertyType<RoleType, 'id'>>;
export type SelectedGrantees = Immutable.List<SelectedGrantee>;

const _sortAndOrderGrantees = <T: GranteeInterface>(grantees: Immutable.List<T>): Immutable.List<T> => {
  const granteesByType = grantees
    .sort((granteeA, granteeB) => defaultCompare(granteeA.title, granteeB.title))
    .groupBy((grantee) => grantee.type);

  return Immutable.List().concat(
    granteesByType.get('global'),
    granteesByType.get('team'),
    granteesByType.get('user'),
  ).filter((grantee) => grantee);
};

type InternalState = {|
  entity: GRN,
  availableGrantees: AvailableGrantees,
  availableRoles: AvailableRoles,
  activeShares: ActiveShares,
  selectedGranteeRoles: SelectedGranteeRoles,
  missingDependencies: Immutable.List<MissingDependency>,
|};

export type EntityShareStateJson = {|
  entity: $PropertyType<InternalState, 'entity'>,
  available_grantees: Array<GranteeType>,
  available_roles: Array<RoleType>,
  active_shares: Array<ActiveShareType>,
  selected_grantee_roles: {|
    [grantee: $PropertyType<Grantee, 'id'>]: $PropertyType<Role, 'id'>,
  |} | {||},
  missing_dependencies: Array<MissingDependencyType>,
|};

export default class EntityShareState {
  _value: InternalState;

  constructor(
    entity: $PropertyType<InternalState, 'entity'>,
    availableGrantees: $PropertyType<InternalState, 'availableGrantees'>,
    availableRoles: $PropertyType<InternalState, 'availableRoles'>,
    activeShares: $PropertyType<InternalState, 'activeShares'>,
    selectedGranteeRoles: $PropertyType<InternalState, 'selectedGranteeRoles'>,
    missingDependencies: $PropertyType<InternalState, 'missingDependencies'>,
  ) {
    this._value = {
      entity,
      availableGrantees: _sortAndOrderGrantees<Grantee>(availableGrantees),
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    };
  }

  get entity(): $PropertyType<InternalState, 'entity'> {
    return this._value.entity;
  }

  get availableGrantees(): $PropertyType<InternalState, 'availableGrantees'> {
    return this._value.availableGrantees;
  }

  get availableRoles(): $PropertyType<InternalState, 'availableRoles'> {
    return this._value.availableRoles;
  }

  get activeShares(): $PropertyType<InternalState, 'activeShares'> {
    return this._value.activeShares;
  }

  get selectedGranteeRoles(): $PropertyType<InternalState, 'selectedGranteeRoles'> {
    return this._value.selectedGranteeRoles;
  }

  get missingDependencies(): $PropertyType<InternalState, 'missingDependencies'> {
    return this._value.missingDependencies;
  }

  get selectedGrantees() {
    const _userLookup = (userId: GRN) => this._value.availableGrantees.find((grantee) => grantee.id === userId);

    const granteesWithRole: Immutable.List<SelectedGrantee> = this._value.selectedGranteeRoles.entrySeq().map(([granteeId, roleId]) => {
      const grantee = _userLookup(granteeId);

      if (!grantee) {
        throw new Error(`Cannot find grantee with id ${granteeId} in available grantees`);
      }

      return SelectedGrantee.create(grantee.id, grantee.title, grantee.type, roleId);
    }).toList();

    return _sortAndOrderGrantees<SelectedGrantee>(granteesWithRole);
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const {
      entity,
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      entity,
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    }));
  }

  toJSON() {
    const {
      entity,
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    } = this._value;

    return {
      entity,
      available_grantees: availableGrantees,
      available_roles: availableRoles,
      active_shares: activeShares,
      selected_grantee_roles: selectedGranteeRoles,
      missing_dependencies: missingDependencies,
    };
  }

  static fromJSON(value: EntityShareStateJson): EntityShareState {
    /* eslint-disable camelcase */
    const {
      entity,
      available_grantees,
      available_roles,
      active_shares,
      selected_grantee_roles,
      missing_dependencies,
    } = value;

    const availableGrantees = Immutable.fromJS(available_grantees.map((ag) => Grantee.fromJSON(ag)));
    const availableRoles = Immutable.fromJS(available_roles.map((ar) => Role.fromJSON(ar)));
    const activeShares = Immutable.fromJS(active_shares.map((as) => ActiveShare.fromJSON(as)));
    const selectedGranteeRoles = Immutable.fromJS(selected_grantee_roles);
    const missingDependencies = Immutable.fromJS(missing_dependencies.map((md) => MissingDependency.fromJSON(md)));

    /* eslint-enable camelcase */
    return new EntityShareState(
      entity,
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    );
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  entity(value: $PropertyType<InternalState, 'entity'>): Builder {
    return new Builder(this.value.set('entity', value));
  }

  availableGrantees(value: $PropertyType<InternalState, 'availableGrantees'>): Builder {
    return new Builder(this.value.set('availableGrantees', value));
  }

  availableRoles(value: $PropertyType<InternalState, 'availableRoles'>): Builder {
    return new Builder(this.value.set('availableRoles', value));
  }

  activeShares(value: $PropertyType<InternalState, 'activeShares'>): Builder {
    return new Builder(this.value.set('activeShares', value));
  }

  selectedGranteeRoles(value: $PropertyType<InternalState, 'selectedGranteeRoles'>): Builder {
    return new Builder(this.value.set('selectedGranteeRoles', value));
  }

  missingDependencies(value: $PropertyType<InternalState, 'missingDependencies'>): Builder {
    return new Builder(this.value.set('missingDependencies', value));
  }

  build(): EntityShareState {
    const {
      entity,
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    } = this.value.toObject();

    return new EntityShareState(
      entity,
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
    );
  }
}
