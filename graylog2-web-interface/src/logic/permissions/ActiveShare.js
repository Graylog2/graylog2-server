// @flow strict
import * as Immutable from 'immutable';

import type { ActiveShare as ActiveShareType } from 'logic/permissions/types';

type InternalState = ActiveShareType;

export default class Grantee {
  _value: InternalState;

  constructor(
    grant: $PropertyType<ActiveShareType, 'grant'>,
    grantee: $PropertyType<ActiveShareType, 'grantee'>,
    role: $PropertyType<ActiveShareType, 'role'>,
  ) {
    this._value = { grant, grantee, role };
  }

  get grant(): $PropertyType<InternalState, 'grant'> {
    return this._value.grant;
  }

  get grantee(): $PropertyType<InternalState, 'grantee'> {
    return this._value.grantee;
  }

  get role(): $PropertyType<InternalState, 'role'> {
    return this._value.role;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { grant, grantee, role } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ grant, grantee, role }));
  }

  toJSON() {
    const { grant, grantee, role } = this._value;

    return { grant, grantee, role };
  }

  static fromJSON(value: ActiveShareType): Grantee {
    const { grant, grantee, role } = value;

    return Grantee
      .builder()
      .grant(grant)
      .grantee(grantee)
      .role(role)
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

  role(value: $PropertyType<InternalState, 'role'>): Builder {
    return new Builder(this.value.set('role', value));
  }

  build(): Grantee {
    const { grant, grantee, role } = this.value.toObject();

    return new Grantee(grant, grantee, role);
  }
}
