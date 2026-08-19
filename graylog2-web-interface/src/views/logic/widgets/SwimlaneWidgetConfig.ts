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

import WidgetConfig from './WidgetConfig';

export const DEFAULT_LIMIT = 500;
export const DEFAULT_MAX_LANES = 20;

export type LaneSortMode = 'eventCount' | 'activity' | 'firstOccurrence' | 'alphabetical' | 'fieldValue';
export const DEFAULT_LANE_SORT: LaneSortMode = 'eventCount';

type InternalState = {
  laneFields: string[];
  colorField: string | undefined;
  shapeField: string | undefined;
  shapeOverrides: Record<string, string>;
  labelField: string | undefined;
  tooltipFields: string[];
  limit: number;
  maxLanes: number;
  laneSort: LaneSortMode;
  laneSortField: string | undefined;
  laneSortAscending: boolean;
};

export type SwimlaneWidgetConfigJSON = {
  lane_fields?: string[];
  /** @deprecated use lane_fields */
  lane_field?: string;
  color_field?: string;
  shape_field?: string;
  shape_overrides?: Record<string, string>;
  label_field?: string;
  tooltip_fields?: string[];
  limit: number;
  max_lanes: number;
  lane_sort?: string;
  lane_sort_field?: string;
  lane_sort_ascending?: boolean;
};

export default class SwimlaneWidgetConfig extends WidgetConfig {
  _value: InternalState;

  constructor(
    laneFields: string[],
    colorField: string | undefined,
    shapeField: string | undefined,
    shapeOverrides: Record<string, string>,
    labelField: string | undefined,
    tooltipFields: string[],
    limit: number,
    maxLanes: number,
    laneSort: LaneSortMode,
    laneSortField: string | undefined,
    laneSortAscending: boolean,
  ) {
    super();
    this._value = { laneFields, colorField, shapeField, shapeOverrides, labelField, tooltipFields, limit, maxLanes, laneSort, laneSortField, laneSortAscending };
  }

  get laneFields() { return this._value.laneFields; }
  get colorField() { return this._value.colorField; }
  get shapeField() { return this._value.shapeField; }
  get shapeOverrides() { return this._value.shapeOverrides; }
  get labelField() { return this._value.labelField; }
  get tooltipFields() { return this._value.tooltipFields; }
  get limit() { return this._value.limit; }
  get maxLanes() { return this._value.maxLanes; }
  get laneSort() { return this._value.laneSort; }
  get laneSortField() { return this._value.laneSortField; }
  get laneSortAscending() { return this._value.laneSortAscending; }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  toJSON(): SwimlaneWidgetConfigJSON {
    const { laneFields, colorField, shapeField, shapeOverrides, labelField, tooltipFields, limit, maxLanes, laneSort, laneSortField, laneSortAscending } = this._value;

    return {
      lane_fields: laneFields,
      color_field: colorField,
      shape_field: shapeField,
      shape_overrides: Object.keys(shapeOverrides).length ? shapeOverrides : undefined,
      label_field: labelField,
      tooltip_fields: tooltipFields.length ? tooltipFields : undefined,
      limit,
      max_lanes: maxLanes,
      lane_sort: laneSort !== DEFAULT_LANE_SORT ? laneSort : undefined,
      lane_sort_field: laneSortField,
      lane_sort_ascending: laneSortAscending ? true : undefined,
    };
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .laneFields([])
      .colorField(undefined)
      .shapeField(undefined)
      .shapeOverrides({})
      .labelField(undefined)
      .tooltipFields([])
      .limit(DEFAULT_LIMIT)
      .maxLanes(DEFAULT_MAX_LANES)
      .laneSort(DEFAULT_LANE_SORT)
      .laneSortField(undefined)
      .laneSortAscending(false);
  }

  static fromJSON(value: SwimlaneWidgetConfigJSON): SwimlaneWidgetConfig {
    const laneFields = value.lane_fields ?? (value.lane_field ? [value.lane_field] : []);

    return new SwimlaneWidgetConfig(
      laneFields,
      value.color_field,
      value.shape_field,
      value.shape_overrides ?? {},
      value.label_field,
      value.tooltip_fields ?? [],
      value.limit ?? DEFAULT_LIMIT,
      value.max_lanes ?? DEFAULT_MAX_LANES,
      (value.lane_sort as LaneSortMode) ?? DEFAULT_LANE_SORT,
      value.lane_sort_field,
      value.lane_sort_ascending ?? false,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  laneFields(v: string[]) { return new Builder(this.value.set('laneFields', v)); }
  colorField(v: string | undefined) { return new Builder(this.value.set('colorField', v)); }
  shapeField(v: string | undefined) { return new Builder(this.value.set('shapeField', v)); }
  shapeOverrides(v: Record<string, string>) { return new Builder(this.value.set('shapeOverrides', v)); }
  labelField(v: string | undefined) { return new Builder(this.value.set('labelField', v)); }
  tooltipFields(v: string[]) { return new Builder(this.value.set('tooltipFields', v)); }
  limit(v: number) { return new Builder(this.value.set('limit', v)); }
  maxLanes(v: number) { return new Builder(this.value.set('maxLanes', v)); }
  laneSort(v: LaneSortMode) { return new Builder(this.value.set('laneSort', v)); }
  laneSortField(v: string | undefined) { return new Builder(this.value.set('laneSortField', v)); }
  laneSortAscending(v: boolean) { return new Builder(this.value.set('laneSortAscending', v)); }

  build() {
    const { laneFields, colorField, shapeField, shapeOverrides, labelField, tooltipFields, limit, maxLanes, laneSort, laneSortField, laneSortAscending } = this.value.toObject();

    return new SwimlaneWidgetConfig(
      laneFields, colorField, shapeField, shapeOverrides ?? {}, labelField, tooltipFields ?? [],
      limit, maxLanes,
      laneSort ?? DEFAULT_LANE_SORT, laneSortField, laneSortAscending ?? false,
    );
  }
}
