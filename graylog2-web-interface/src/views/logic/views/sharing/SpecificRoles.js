// @flow strict
import { Map } from 'immutable';

import ViewSharing from './ViewSharing';

type SpecificRolesJson = {|
  type: string,
  view_id: string,
  roles: Array<string>,
|};

export default class SpecificRoles extends ViewSharing {
  static Type = 'roles';

  _roles: Array<string>;

  constructor(viewId: string, roles: Array<string>) {
    super(viewId);
    this._roles = roles;
  }

  // eslint-disable-next-line class-methods-use-this
  get type() {
    return SpecificRoles.Type;
  }

  get roles() {
    return this._roles;
  }

  // eslint-disable-next-line no-use-before-define
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

  static create(viewId: string, roles: Array<string>) {
    return new SpecificRoles(viewId, roles);
  }

  static fromJSON(value: SpecificRolesJson) {
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

  build() {
    const { viewId, roles } = this.value.toObject();

    return SpecificRoles.create(viewId, roles);
  }
}
