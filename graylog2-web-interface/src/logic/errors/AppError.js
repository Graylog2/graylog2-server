// @flow strict
type RuntimeError = 'RuntimeError'
type UnatherizedError = 'UnauthorizedError'

type InternalState = {
  error: any,
  type: RuntimeError | UnatherizedError,
  componentStack?: string
}

export default class AppError {
  static Type: { Runtime: RuntimeError, Unauthorized: UnatherizedError } = {
    Runtime: 'RuntimeError',
    Unauthorized: 'UnauthorizedError',
  }

  _value: InternalState;

  constructor(error: any, type: RuntimeError | UnatherizedError, componentStack?: string) {
    this._value = { error, type, componentStack };
  }

  get componentStack() { return this._value.componentStack; }

  get type() { return this._value.type; }

  get error() { return this._value.error; }
}
