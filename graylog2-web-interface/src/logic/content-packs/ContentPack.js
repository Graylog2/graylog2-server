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
import { Map, Set } from 'immutable';
import { concat, remove } from 'lodash';
import uuid from 'uuid/v4';

import Entity from './Entity';

export default class ContentPack {
  constructor(v, id, rev, name, summary, description, vendor, url, parameters, entitieValues) {
    const entities = entitieValues.map((e) => {
      if (e instanceof Entity) {
        return e;
      }

      return Entity.fromJSON(e, false, parameters);
    });

    this._value = {
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    };
  }

  get v() {
    return this._value.v;
  }

  get id() {
    return this._value.id;
  }

  get rev() {
    return this._value.rev;
  }

  get name() {
    return this._value.name;
  }

  get summary() {
    return this._value.summary;
  }

  get description() {
    return this._value.description;
  }

  get vendor() {
    return this._value.vendor;
  }

  get url() {
    return this._value.url;
  }

  get constraints() {
    return this._value.entities.reduce((acc, entity) => {
      return entity.constraints.reduce((result, constraint) => {
        return result.add(constraint);
      }, acc);
    }, Set());
  }

  get parameters() {
    return this._value.parameters;
  }

  get entities() {
    return this._value.entities;
  }

  toBuilder() {
    const {
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    }));
  }

  toJSON() {
    const {
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    } = this._value;

    const entitiesJSON = entities.map((e) => e.toJSON());

    return {
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities: entitiesJSON,
    };
  }

  static fromJSON(value) {
    const {
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    } = value;

    return new ContentPack(
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    );
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .v(1)
      .id(uuid())
      .rev(1)
      .name('')
      .summary('')
      .description('')
      .vendor('')
      .url('')
      .parameters([])
      .entities([]);
  }
}

class Builder {
  constructor(value = Map()) {
    this.value = value;
  }

  v(value) {
    this.value = this.value.set('v', value);

    return this;
  }

  id(value) {
    this.value = this.value.set('id', value);

    return this;
  }

  rev(value) {
    this.value = this.value.set('rev', value);

    return this;
  }

  name(value) {
    this.value = this.value.set('name', value);

    return this;
  }

  summary(value) {
    this.value = this.value.set('summary', value);

    return this;
  }

  description(value) {
    this.value = this.value.set('description', value);

    return this;
  }

  vendor(value) {
    this.value = this.value.set('vendor', value);

    return this;
  }

  url(value) {
    this.value = this.value.set('url', value);

    return this;
  }

  parameters(value) {
    this.value = this.value.set('parameters', value);

    return this;
  }

  removeParameter(value) {
    const parameters = this.value.get('parameters').slice(0);

    remove(parameters, (parameter) => parameter.name === value.name);
    this.value = this.value.set('parameters', parameters);

    return this;
  }

  addParameter(value) {
    const parameters = this.value.get('parameters');
    const newParameters = concat(parameters, value);

    this.value = this.value.set('parameters', newParameters);

    return this;
  }

  entities(value) {
    this.value = this.value.set('entities', value);

    return this;
  }

  build() {
    const {
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    } = this.value.toObject();

    return new ContentPack(
      v,
      id,
      rev,
      name,
      summary,
      description,
      vendor,
      url,
      parameters,
      entities,
    );
  }
}
