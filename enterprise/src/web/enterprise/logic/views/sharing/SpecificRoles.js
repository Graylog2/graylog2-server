import { Map } from 'immutable';

import ViewSharing from './ViewSharing';

type SpecificRolesJson = {
  type: 'roles',
  view_id: string,
  roles: Array<string>,
};

export default class SpecificRoles extends ViewSharing {
  static Type = 'roles';

  constructor(viewId, roles) {
    super(viewId);
    this._value.roles = roles;
  }

  get type() {
    return SpecificRoles.Type;
  }

  get roles() {
    return this._value.roles;
  }

  toBuilder(): Builder {
    const { viewId, roles } = this;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ viewId, roles }));
  }

  toJSON(): SpecificRolesJson {
    return {
      type: SpecificRoles.Type,
      view_id: this.viewId,
      roles: this.roles,
    };
  }

  static create(viewId, roles) {
    return new SpecificRoles(viewId, roles);
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { view_id, roles } = value;
    return SpecificRoles.create(view_id, roles);
  }
}

class Builder {
  value: Map<string, any>;

  constructor(value: Map<string, any> = Map()) {
    this.value = value;
  }

  roles(value: Array<string>) {
    this.value = this.value.set('roles', value);
    return this;
  }

  build(): SpecificUsers {
    const { viewId, roles } = this.value.toObject();
    return SpecificRoles.create(viewId, roles);
  }
}
