// @flow strict
import * as Immutable from 'immutable';

import type {
  GranteeType,
  CapabilityType,
  SharedEntityType,
  ActiveShareType,
  GRN,
} from 'logic/permissions/types';
import { defaultCompare } from 'views/logic/DefaultCompare';

import Capability from './Capability';
import Grantee from './Grantee';
import ActiveShare from './ActiveShare';
import SharedEntity from './SharedEntity';
import SelectedGrantee from './SelectedGrantee';
import type { GranteeInterface } from './GranteeInterface';
import ValidationResult, { type ValidationResultJSON } from './ValidationResult';

export type GranteesList = Immutable.List<Grantee>;
export type CapabilitiesList = Immutable.List<Capability>;
export type ActiveShares = Immutable.List<ActiveShare>;
export type MissingDependencies = Immutable.Map<GRN, Immutable.List<SharedEntity>>;
export type SelectedGranteeCapabilities = Immutable.Map<$PropertyType<GranteeType, 'id'>, $PropertyType<CapabilityType, 'id'>>;
export type SelectedGrantees = Immutable.List<SelectedGrantee>;

const _missingDependenciesFromJSON = (missingDependenciesJSON) => {
  let missingDependencies = Immutable.Map();

  Object.keys(missingDependenciesJSON).forEach((granteeGRN) => {
    const dependencyList = missingDependenciesJSON[granteeGRN];
    missingDependencies = missingDependencies.set(granteeGRN, dependencyList.map((dependency) => SharedEntity.fromJSON(dependency)));
  });

  return missingDependencies;
};

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
  availableGrantees: GranteesList,
  availableCapabilities: CapabilitiesList,
  activeShares: ActiveShares,
  selectedGranteeCapabilities: SelectedGranteeCapabilities,
  missingDependencies: MissingDependencies,
  validationResults: ValidationResult,
|};

export type EntityShareStateJson = {|
  entity: $PropertyType<InternalState, 'entity'>,
  available_grantees: Array<GranteeType>,
  available_capabilities: Array<CapabilityType>,
  active_shares: Array<ActiveShareType>,
  selected_grantee_capabilities: {|
    [grantee: $PropertyType<Grantee, 'id'>]: $PropertyType<Capability, 'id'>,
  |} | {||},
  missing_permissions_on_dependencies: {[GRN]: Array<SharedEntityType>},
  validation_result: ValidationResultJSON,
|};

export default class EntityShareState {
  _value: InternalState;

  constructor(
    entity: $PropertyType<InternalState, 'entity'>,
    availableGrantees: $PropertyType<InternalState, 'availableGrantees'>,
    availableCapabilities: $PropertyType<InternalState, 'availableCapabilities'>,
    activeShares: $PropertyType<InternalState, 'activeShares'>,
    selectedGranteeCapabilities: $PropertyType<InternalState, 'selectedGranteeCapabilities'>,
    missingDependencies: $PropertyType<InternalState, 'missingDependencies'>,
    validationResults: $PropertyType<InternalState, 'validationResults'>,
  ) {
    this._value = {
      entity,
      availableGrantees: _sortAndOrderGrantees<Grantee>(availableGrantees),
      availableCapabilities,
      activeShares,
      selectedGranteeCapabilities,
      missingDependencies,
      validationResults,
    };
  }

  get entity(): $PropertyType<InternalState, 'entity'> {
    return this._value.entity;
  }

  get availableGrantees(): $PropertyType<InternalState, 'availableGrantees'> {
    return this._value.availableGrantees;
  }

  get availableCapabilities(): $PropertyType<InternalState, 'availableCapabilities'> {
    return this._value.availableCapabilities;
  }

  get activeShares(): $PropertyType<InternalState, 'activeShares'> {
    return this._value.activeShares;
  }

  get selectedGranteeCapabilities(): $PropertyType<InternalState, 'selectedGranteeCapabilities'> {
    return this._value.selectedGranteeCapabilities;
  }

  get missingDependencies(): $PropertyType<InternalState, 'missingDependencies'> {
    return this._value.missingDependencies;
  }

  get validationResults(): $PropertyType<InternalState, 'validationResults'> {
    return this._value.validationResults;
  }

  get selectedGrantees() {
    const _userLookup = (userId: GRN) => this._value.availableGrantees.find((grantee) => grantee.id === userId);

    const granteesWithCapabilities: Immutable.List<SelectedGrantee> = this._value.selectedGranteeCapabilities.entrySeq().map(([granteeId, roleId]) => {
      const grantee = _userLookup(granteeId);

      if (!grantee) {
        throw new Error(`Cannot find grantee with id ${granteeId} in available grantees`);
      }

      return SelectedGrantee.create(grantee.id, grantee.title, grantee.type, roleId);
    }).toList();

    return _sortAndOrderGrantees<SelectedGrantee>(granteesWithCapabilities);
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const {
      entity,
      availableGrantees,
      availableCapabilities,
      activeShares,
      selectedGranteeCapabilities,
      missingDependencies,
      validationResults,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      entity,
      availableGrantees,
      availableCapabilities,
      activeShares,
      selectedGranteeCapabilities,
      missingDependencies,
      validationResults,
    }));
  }

  toJSON() {
    const {
      entity,
      availableGrantees = Immutable.List(),
      availableCapabilities = Immutable.List(),
      activeShares = Immutable.List(),
      selectedGranteeCapabilities = Immutable.Map(),
      missingDependencies = Immutable.Map(),
      validationResults,
    } = this._value;

    return {
      entity,
      available_grantees: availableGrantees.toJS(),
      available_capabilities: availableCapabilities.toJS(),
      active_shares: activeShares.toJS(),
      selected_grantee_capabilities: selectedGranteeCapabilities.toJS(),
      missing_permissions_on_dependencies: missingDependencies.toJS(),
      validation_result: validationResults,
    };
  }

  static fromJSON(value: EntityShareStateJson): EntityShareState {
    /* eslint-disable camelcase */
    const {
      entity,
      available_grantees,
      available_capabilities,
      active_shares,
      selected_grantee_capabilities,
      missing_permissions_on_dependencies,
      validation_result,
    } = value;

    const availableGrantees = Immutable.fromJS(available_grantees.map((ag) => Grantee.fromJSON(ag)));
    const availableCapabilities = Immutable.fromJS(available_capabilities.map((ar) => Capability.fromJSON(ar)));
    const activeShares = Immutable.fromJS(active_shares.map((as) => ActiveShare.fromJSON(as)));
    const selectedGranteeCapabilities = Immutable.fromJS(selected_grantee_capabilities);
    const missingDependencies = _missingDependenciesFromJSON(missing_permissions_on_dependencies);
    const validationResults = ValidationResult.fromJSON(validation_result);

    /* eslint-enable camelcase */
    return new EntityShareState(
      entity,
      availableGrantees,
      availableCapabilities,
      activeShares,
      selectedGranteeCapabilities,
      missingDependencies,
      validationResults,
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

  availableCapabilities(value: $PropertyType<InternalState, 'availableCapabilities'>): Builder {
    return new Builder(this.value.set('availableCapabilities', value));
  }

  activeShares(value: $PropertyType<InternalState, 'activeShares'>): Builder {
    return new Builder(this.value.set('activeShares', value));
  }

  selectedGranteeCapabilities(value: $PropertyType<InternalState, 'selectedGranteeCapabilities'>): Builder {
    return new Builder(this.value.set('selectedGranteeCapabilities', value));
  }

  missingDependencies(value: $PropertyType<InternalState, 'missingDependencies'>): Builder {
    return new Builder(this.value.set('missingDependencies', value));
  }

  validationResults(value: $PropertyType<InternalState, 'validationResults'>): Builder {
    return new Builder(this.value.set('validationResults', value));
  }

  build(): EntityShareState {
    const {
      entity,
      availableGrantees,
      availableCapabilities,
      activeShares,
      selectedGranteeCapabilities,
      missingDependencies,
      validationResults,
    } = this.value.toObject();

    return new EntityShareState(
      entity,
      availableGrantees,
      availableCapabilities,
      activeShares,
      selectedGranteeCapabilities,
      missingDependencies,
      validationResults,
    );
  }
}
