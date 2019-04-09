// @flow strict

import { Map, Collection, fromJS, is } from 'immutable';
import { fromPairs } from 'lodash';

import Widget from 'enterprise/logic/widgets/Widget';
import WidgetPosition from 'enterprise/logic/widgets/WidgetPosition';
import TitleTypes from 'enterprise/stores/TitleTypes';
import type { TitlesMap } from 'enterprise/stores/TitleTypes';

type FieldNameList = Array<string>;
type WidgetMapping = Map<string, Collection<string>>;
type State = {
  fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<Widget>,
  widgetMapping: WidgetMapping,
  widgetPositions: { [string]: WidgetPosition },
};

type BuilderState = Map<string, any>;

type JsonState = {
  selected_fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<any>,
  widget_mapping: WidgetMapping,
  positions: { [string]: WidgetPosition },
};

export default class ViewState {
  _value: State;

  constructor(fields: FieldNameList, titles: TitlesMap, widgets: Array<Widget>, widgetMapping: WidgetMapping, widgetPositions: { [string]: WidgetPosition }) {
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

  get widgetPositions(): { [string]: WidgetPosition } {
    return this._value.widgetPositions;
  }

  duplicate() {
    const widgetIdTranslation = {};
    const newWidgets = this.widgets.map((widget) => {
      const newWidget = widget.toBuilder().newId().build();
      widgetIdTranslation[widget.id] = newWidget.id;
      return newWidget;
    });
    const newWidgetTitles = this.titles.get(TitleTypes.Widget).mapEntries(([key, value]) => [widgetIdTranslation[key], value]);
    const newTitles = this.titles
      .set(TitleTypes.Widget, newWidgetTitles)
      .updateIn([TitleTypes.Tab, 'title'], value => (value ? `${value} (Copy)` : value));
    const newWidgetPositions = Map(this.widgetPositions).mapEntries(([key, value]) => [widgetIdTranslation[key], value]).toJS();
    return this.toBuilder()
      .widgetMapping(Map())
      .widgetPositions(newWidgetPositions)
      .widgets(newWidgets)
      .titles(newTitles)
      .build();
  }

  // eslint-disable-next-line no-use-before-define
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
    return ViewState.builder()
      .titles(fromJS(titles))
      .widgets(widgets.map(w => Widget.fromJSON(w)))
      .widgetMapping(fromJS(widget_mapping))
      .fields(selected_fields)
      .widgetPositions(fromPairs(Object.entries(positions).map(([k, v]) => ([k, WidgetPosition.fromJSON(v)]))))
      .build();
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
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
