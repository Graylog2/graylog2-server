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
import * as Immutable from 'immutable';

import VisualizationConfig from './VisualizationConfig';

export type PinnedColumns = Array<string>

export type DataTableVisualizationConfigType = {
  pinnedColumns: PinnedColumns,
};

export type DataTableVisualizationConfigTypeJSON = {
  pinned_columns: PinnedColumns,
};

export default class DataTableVisualizationConfig extends VisualizationConfig {
  _value: DataTableVisualizationConfigType;

  constructor(pinnedColumns: PinnedColumns) {
    super();
    this._value = { pinnedColumns: pinnedColumns || [] };
  }

  static empty() {
    return this.create([]);
  }

  get pinnedColumns() {
    return Immutable.Set(this._value.pinnedColumns);
  }

  toBuilder() {
    const { pinnedColumns } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ pinnedColumns }));
  }

  static create(pinnedColumns: PinnedColumns) {
    return new DataTableVisualizationConfig(pinnedColumns);
  }

  toJSON() {
    const { pinnedColumns } = this._value;

    return {
      pinned_columns: pinnedColumns,
    };
  }

  static fromJSON(_type: string, value: DataTableVisualizationConfigTypeJSON) {
    const { pinned_columns } = value;

    return DataTableVisualizationConfig.create(pinned_columns);
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  pinnedColumns(value: PinnedColumns) {
    return new Builder(this.value.set('pinnedColumns', value));
  }

  build() {
    const { pinnedColumns } = this.value.toObject();

    return new DataTableVisualizationConfig(pinnedColumns);
  }
}
