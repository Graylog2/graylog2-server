// @flow strict
import * as Immutable from 'immutable';

import type { ActiveShare as ActiveShareType } from 'logic/permissions/types';

type InternalState = ActiveShareType;

export default class Grantee {
  _value: InternalState;

  constructor(
    grant: $PropertyType<ActiveShareType, 'grant'>,
    grantee: $PropertyType<ActiveShareType, 'grantee'>,
    capability: $PropertyType<ActiveShareType, 'capability'>,
  ) {
    this._value = { grant, grantee, capability };
  }

  get grant(): $PropertyType<InternalState, 'grant'> {
    return this._value.grant;
  }

  get grantee(): $PropertyType<InternalState, 'grantee'> {
    return this._value.grantee;
  }

  get capability(): $PropertyType<InternalState, 'capability'> {
    return this._value.capability;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { grant, grantee, capability } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ grant, grantee, capability }));
  }

  toJSON() {
    const { grant, grantee, capability } = this._value;

    return { grant, grantee, capability };
  }

  static fromJSON(value: ActiveShareType): Grantee {
    const { grant, grantee, capability } = value;

    return Grantee
      .builder()
      .grant(grant)
      .grantee(grantee)
      .capability(capability)
      .build();
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

  grant(value: $PropertyType<InternalState, 'grant'>): Builder {
    return new Builder(this.value.set('grant', value));
  }

  grantee(value: $PropertyType<InternalState, 'grantee'>): Builder {
    return new Builder(this.value.set('grantee', value));
  }

  capability(value: $PropertyType<InternalState, 'capability'>): Builder {
    return new Builder(this.value.set('capability', value));
  }

  build(): Grantee {
    const { grant, grantee, capability } = this.value.toObject();

    return new Grantee(grant, grantee, capability);
  }
}
