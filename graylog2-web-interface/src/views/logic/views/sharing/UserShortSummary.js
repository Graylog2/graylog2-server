// @flow strict

type UserShortSummaryJson = {
  username: string,
  full_name: string,
};

type State = {
  username: string,
  fullname: string,
};

export default class UserShortSummary {
  _value: State;

  constructor(username: string, fullname: string) {
    this._value = { username, fullname };
  }

  get username() {
    return this._value.username;
  }

  get fullname() {
    return this._value.fullname;
  }

  static fromJSON(value: UserShortSummaryJson) {
    const { username, full_name: fullname } = value;

    return new UserShortSummary(username, fullname);
  }
}
