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
// @flow strict

export type VisualizationConfigJson = {
  type: string,
};

export default class VisualizationConfig {
  static fromJSON(type: string, value: any): VisualizationConfig {
    const implementingClass = VisualizationConfig.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(type, value);
    }

    throw new Error(`Unable to find visualization config of type: ${type} - missing plugin?`);
  }

  // eslint-disable-next-line class-methods-use-this
  toBuilder() {
    throw new Error('Must not be called on abstract class!');
  }

  static __registrations: { [string]: typeof VisualizationConfig } = {};

  static registerSubtype(type: string, implementingClass: typeof VisualizationConfig) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }
}
