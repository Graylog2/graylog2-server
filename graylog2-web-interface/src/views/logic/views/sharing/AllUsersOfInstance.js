import ViewSharing from './ViewSharing';

type AllUsersOfInstanceJson = {
  type: 'all_of_instance',
  view_id: string,
};

export default class AllUsersOfInstance extends ViewSharing {
  static Type = 'all_of_instance';

  get type() {
    return AllUsersOfInstance.Type;
  }

  static create(viewId) {
    return new AllUsersOfInstance(viewId);
  }

  toJSON(): AllUsersOfInstanceJson {
    return {
      type: AllUsersOfInstance.Type,
      view_id: this.viewId,
    };
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { view_id } = value;
    return AllUsersOfInstance.create(view_id);
  }
}
