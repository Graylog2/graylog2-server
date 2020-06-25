// @flow strict
import { Map } from 'immutable';

import ViewSharing from './ViewSharing';

type SpecificUsersJson = {|
  type: string,
  view_id: string,
  users: Array<string>,
|};

export default class SpecificUsers extends ViewSharing {
  static Type = 'users';

  _users: Array<string>;

  constructor(viewId: string, users: Array<string>) {
    super(viewId);
    this._users = users;
  }

  // eslint-disable-next-line class-methods-use-this
  get type() {
    return SpecificUsers.Type;
  }

  get users() {
    return this._users;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { viewId, users } = this;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ viewId, users }));
  }

  toJSON(): SpecificUsersJson {
    return {
      type: SpecificUsers.Type,
      view_id: this.viewId,
      users: this.users,
    };
  }

  static create(viewId: string, users: Array<string>) {
    return new SpecificUsers(viewId, users);
  }

  static fromJSON(value: SpecificUsersJson) {
    // eslint-disable-next-line camelcase
    const { view_id, users } = value;

    return SpecificUsers.create(view_id, users);
  }
}

class Builder {
  value: Map<string, any>;

  constructor(value: Map<string, any> = Map()) {
    this.value = value;
  }

  users(value: Array<string>) {
    this.value = this.value.set('users', value);

    return this;
  }

  build(): SpecificUsers {
    const { viewId, users } = this.value.toObject();

    return SpecificUsers.create(viewId, users);
  }
}
