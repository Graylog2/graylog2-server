// @flow strict
type InternalState = {
  viewId: string,
};

export type ViewSharingJson = {|
  view_id: string,
  type: string,
  [string]: any,
|};

export default class ViewSharing {
  _value: InternalState;

  constructor(viewId: string) {
    this._value = { viewId };
  }

  static __registrations: { [string]: typeof ViewSharing } = {};

  // eslint-disable-next-line class-methods-use-this
  get type(): string {
    return 'unknown';
  }

  get viewId() {
    return this._value.viewId;
  }

  // eslint-disable-next-line no-unused-vars
  static create(viewId: string, ...rest: any) {
    return new ViewSharing(viewId);
  }

  toJSON() {
    const { type, viewId } = this;

    return {
      type,
      view_id: viewId,
    };
  }

  static fromJSON(value: ViewSharingJson) {
    // eslint-disable-next-line camelcase
    const { type, view_id } = value;

    const implementingClass = ViewSharing.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(value);
    }

    return ViewSharing.create(view_id);
  }

  static registerSubtype(type: string, implementingClass: typeof ViewSharing) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }
}
