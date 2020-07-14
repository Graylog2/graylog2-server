// @flow strict
import ViewSharing from './ViewSharing';

type AllUsersOfInstanceJson = {
  type: string,
  view_id: string,
};

export default class AllUsersOfInstance extends ViewSharing {
  static Type = 'all_of_instance';

  // eslint-disable-next-line class-methods-use-this
  get type() {
    return AllUsersOfInstance.Type;
  }

  static create(viewId: string) {
    return new AllUsersOfInstance(viewId);
  }

  toJSON(): AllUsersOfInstanceJson {
    return {
      type: AllUsersOfInstance.Type,
      view_id: this.viewId,
    };
  }

  static fromJSON(value: AllUsersOfInstanceJson) {
    // eslint-disable-next-line camelcase
    const { view_id } = value;

    return AllUsersOfInstance.create(view_id);
  }
}
