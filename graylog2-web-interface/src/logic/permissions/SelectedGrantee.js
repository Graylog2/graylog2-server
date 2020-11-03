// @flow strict
import { type GranteeInterface } from './GranteeInterface';
import Grantee from './Grantee';
import Capability from './Capability';
import type { ActiveShares } from './EntityShareState';

type InternalState = {
  id: $PropertyType<Grantee, 'id'>,
  title: $PropertyType<Grantee, 'title'>,
  type: $PropertyType<Grantee, 'type'>,
  capabilityId: $PropertyType<Capability, 'id'>,
};

export type CurrentState = 'new' | 'changed' | 'unchanged';

export default class SelectedGrantee implements GranteeInterface {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    type: $PropertyType<InternalState, 'type'>,
    capabilityId: $PropertyType<InternalState, 'capabilityId'>,
  ) {
    this._value = { id, title, type, capabilityId };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  get type(): $PropertyType<InternalState, 'type'> {
    return this._value.type;
  }

  get capabilityId(): $PropertyType<InternalState, 'capabilityId'> {
    return this._value.capabilityId;
  }

  currentState(activeShares: ActiveShares): CurrentState {
    const { capabilityId, id } = this._value;
    const activeShare = activeShares.find((share) => share.grantee === id);

    if (!activeShare) return 'new';
    if (activeShare.capability !== capabilityId) return 'changed';

    return 'unchanged';
  }

  static create(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    type: $PropertyType<InternalState, 'type'>,
    capabilityId: $PropertyType<InternalState, 'capabilityId'>,
  ) {
    return new SelectedGrantee(id, title, type, capabilityId);
  }
}
