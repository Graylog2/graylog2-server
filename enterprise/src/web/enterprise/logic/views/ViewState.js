// @flow

import { Map, fromJS, is } from 'immutable';
import { fromPairs } from 'lodash';

import Widget from 'enterprise/logic/widgets/Widget';
import WidgetPosition from 'enterprise/logic/widgets/WidgetPosition';

type FieldNameList = Array<string>;
type TitlesMap = Map<string, Map<string, string>>;
type WidgetMapping = Map<string, string>;
type State = {
  fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<Widget>,
  widgetMapping: WidgetMapping,
  widgetPositions: Array<WidgetPosition>
};

type BuilderState = Map<string, any>;

type JsonState = {
  selected_fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<any>,
  widget_mapping: WidgetMapping,
  positions: Array<WidgetPosition>
};

export default class ViewState {
  _value: State;

  constructor(fields: FieldNameList, titles: TitlesMap, widgets: Array<Widget>, widgetMapping: WidgetMapping, widgetPositions: Array<WidgetPosition>) {
    this._value = { fields, titles, widgets, widgetMapping, widgetPositions };
  }

  static create(): ViewState {
    // eslint-disable-next-line no-use-before-define
    return new Builder().widgets([]).widgetPositions({}).build();
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

  equals(other: any) {
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
      fromPairs(Object.entries(positions).map(([k, v]) => ([k, WidgetPosition.fromJSON(v)]))),
    );
  }
}

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Map()) {
    this.value = value;
  }

  fields(value: FieldNameList): Builder {
    return new Builder(this.value.set('fields', value));
  }

  titles(value: TitlesMap): Builder {
    return new Builder(this.value.set('titles', fromJS(value)));
  }

  widgets(value: Array<Widget>): Builder {
    return new Builder(this.value.set('widgets', value));
  }

  widgetMapping(value: WidgetMapping): Builder {
    return new Builder(this.value.set('widgetMapping', value));
  }

  widgetPositions(value: Map<string, WidgetPosition>): Builder {
    return new Builder(this.value.set('widgetPositions', value));
  }

  build(): ViewState {
    const { fields, titles, widgets, widgetMapping, widgetPositions } = this.value.toObject();
    return new ViewState(fields, titles, widgets, widgetMapping, widgetPositions);
  }
}
