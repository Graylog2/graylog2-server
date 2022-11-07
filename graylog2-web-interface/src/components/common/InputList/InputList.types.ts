/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
export type GenericTarget<T> = Omit<HTMLInputElement, 'value'> & {
  name: string,
  value: T,
}

export class GenericChangeEvent<T> extends Event implements React.ChangeEvent<GenericTarget<T>> {
  _target: GenericTarget<T>;

  _defaultPrevented: boolean;

  _propagationStopped: boolean;

  _persisted: boolean;

  nativeEvent: Event;

  set target(element: GenericTarget<T>) {
    this._target = element;
  }

  get target() {
    return this._target;
  }

  set currentTarget(element: GenericTarget<T>) {
    this._target = element;
  }

  get currentTarget() {
    return this._target;
  }

  preventDefault() {
    super.preventDefault();
    this._defaultPrevented = true;
  }

  isDefaultPrevented() {
    return this._defaultPrevented;
  }

  stopPropagation() {
    super.stopPropagation();
    this._propagationStopped = true;
  }

  isPropagationStopped() {
    return this._propagationStopped;
  }

  persist() {
    this._persisted = true;
  }
}
