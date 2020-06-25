// @flow strict
import * as Immutable from 'immutable';

import type {
  Grantee as GranteeType,
  Role as RoleType,
  MissingDependency as MissingDependencyType,
  ActiveShare as ActiveShareType,
  SelectedGranteeRoles,
  GRN,
} from 'logic/permissions/types';

import Role from './Role';
import Grantee from './Grantee';
import ActiveShare from './ActiveShare';
import MissingDependency from './MissingDependency';

type InternalState = {|
  entity: GRN,
  availableGrantees: Immutable.List<Grantee>,
  availableRoles: Immutable.List<Role>,
  activeShares: Immutable.List<ActiveShare>,
  selectedGranteeRoles: SelectedGranteeRoles,
  missingDependencies: Immutable.List<MissingDependency>,
|};

type EntityShareStateJson = {|
  entity: $PropertyType<InternalState, 'entity'>,
  available_grantees: Immutable.List<GranteeType>,
  available_roles: Immutable.List<RoleType>,
  active_shares: Immutable.List<ActiveShareType>,
  selected_grantee_roles: $PropertyType<InternalState, 'selectedGranteeRoles'>,
  missing_dependencies: Immutable.List<MissingDependencyType>,
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
      availableGrantees: Immutable.fromJS(availableGrantees),
      availableRoles: Immutable.fromJS(availableRoles),
      activeShares: Immutable.fromJS(activeShares),
      selectedGranteeRoles,
      missingDependencies: Immutable.fromJS(missingDependencies),
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
      availableGrantees,
      availableRoles,
      activeShares,
      selectedGranteeRoles,
      missingDependencies,
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
    /* eslint-enable camelcase */
    return new EntityShareState(
      entity,
      available_grantees.map((ag) => Grantee.fromJSON(ag)),
      available_roles.map((ar) => Role.fromJSON(ar)),
      active_shares.map((as) => ActiveShare.fromJSON(as)),
      selected_grantee_roles,
      missing_dependencies.map((md) => MissingDependency.fromJSON(md)),
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
