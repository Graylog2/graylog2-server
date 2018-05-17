// @flow

import { Map, fromJS, is } from 'immutable';
import Widget from 'enterprise/logic/widgets/Widget';

type FieldNameList = Array<string>;
type TitlesMap = Map<string, Map<string, string>>;
type WidgetMapping = Map<string, string>;
type WidgetPosition = { col: number, row: number, width: number, height: number };
type State = {
  fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<Widget>,
  widgetMapping: WidgetMapping,
  widgetPositions: Array<WidgetPosition>
};

type JsonState = {
  selected_fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<any>,
  widget_mapping: WidgetMapping,
  widget_positions: Array<WidgetPosition>
};

export default class ViewState {
  _value: State;

  constructor(fields: FieldNameList, titles: TitlesMap, widgets: Array<Widget>, widgetMapping: WidgetMapping, widgetPositions: Array<WidgetPosition>) {
    this._value = { fields, titles, widgets, widgetMapping, widgetPositions };
  }

  static create(): ViewState {
    return new ViewState([], {}, [], {}, []);
  }

  get fields(): FieldNameList {
    return this._value.fields;
  }

  get titles(): TitlesMap {
    return this._value.titles;
  }

  get widgets(): Array<Widget> {
    return this._value.widgets;
  }

  get widgetMapping(): WidgetMapping {
    return this._value.widgetMapping;
  }

  get widgetPositions(): Array<WidgetPosition> {
    return this._value.widgetPositions;
  }

  toBuilder(): Builder {
    const { fields, titles, widgets, widgetMapping, widgetPositions } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({ fields, titles, widgets, widgetMapping, widgetPositions }));
  }

  equals(other) {
    if (other === undefined) {
      return false;
    }
    if (!(other instanceof ViewState)) {
      return false;
    }

    if (this.fields !== other.fields || !is(this.titles, other.titles) || this.widgets !== other.widgets || !is(this.widgetMapping, other.widgetMapping) || this.widgetPositions !== other.widgetPositions) {
      return false;
    }

    return true;
  }

  toJSON() {
    const { fields, titles, widgets, widgetMapping, widgetPositions } = this._value;
    return {
      selected_fields: fields,
      titles,
      widgets,
      widget_mapping: widgetMapping,
      positions: widgetPositions,
    };
  }

  static fromJSON(value: JsonState): ViewState {
    // eslint-disable-next-line camelcase
    const { selected_fields, titles, widgets, widget_mapping, positions } = value;
    return new ViewState(
      selected_fields,
      fromJS(titles),
      widgets.map(w => Widget.fromJSON(w)),
      widget_mapping,
      positions,
    );
  }
}

class Builder {
  value: Map<string, any>;

  constructor(value = Map()) {
    this.value = value;
  }

  fields(value: FieldNameList) {
    return new Builder(this.value.set('fields', value));
  }

  titles(value: TitlesMap) {
    return new Builder(this.value.set('titles', fromJS(value)));
  }

  widgets(value: Array<Widget>) {
    return new Builder(this.value.set('widgets', value));
  }

  widgetMapping(value: WidgetMapping) {
    return new Builder(this.value.set('widgetMapping', value));
  }

  widgetPositions(value: Array<WidgetPosition>) {
    return new Builder(this.value.set('widgetPositions', value));
  }

  build(): ViewState {
    const { fields, titles, widgets, widgetMapping, widgetPositions } = this.value.toObject();
    return new ViewState(fields, titles, widgets, widgetMapping, widgetPositions);
  }
}