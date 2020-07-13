// @flow strict
import { List, Map, fromJS } from 'immutable';

import isDeepEqual from 'stores/isDeepEqual';
import Widget from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import TitleTypes from 'views/stores/TitleTypes';
import type { TitlesMap, TitleType } from 'views/stores/TitleTypes';
import type { WidgetPositionJSON } from 'views/logic/widgets/WidgetPosition';

import type { FormattingSettingsJSON } from './formatting/FormattingSettings';
import FormattingSettings from './formatting/FormattingSettings';
import type { WidgetMapping } from './types';

type FieldNameList = Array<string>;
type State = {
  fields: FieldNameList,
  formatting: FormattingSettings,
  titles: TitlesMap,
  widgets: List<Widget>,
  widgetMapping: WidgetMapping,
  widgetPositions: Map<string, WidgetPosition>,
  staticMessageListId?: string,
};

type BuilderState = Map<string, any>;

export type ViewStateJson = {
  formatting?: FormattingSettingsJSON,
  positions: { [string]: WidgetPositionJSON },
  selected_fields: FieldNameList,
  titles: TitlesMap,
  widgets: Array<any>,
  widget_mapping: WidgetMapping,
  staticMessageListId?: string,
};

export default class ViewState {
  _value: State;

  constructor(fields: FieldNameList,
    titles: TitlesMap,
    widgets: Array<Widget>,
    widgetMapping: WidgetMapping,
    widgetPositions: { [string]: WidgetPosition },
    formatting: FormattingSettings,
    staticMessageListId?: string) {
    this._value = { fields, titles, widgets: List(widgets), widgetMapping, widgetPositions: Map(widgetPositions), formatting, staticMessageListId };
  }

  static create(): ViewState {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .widgets(List())
      .widgetPositions(Map())
      .titles(Map())
      .build();
  }

  get fields(): FieldNameList {
    return this._value.fields;
  }

  get formatting(): FormattingSettings {
    return this._value.formatting;
  }

  get titles(): TitlesMap {
    return this._value.titles;
  }

  get widgets(): List<Widget> {
    return this._value.widgets;
  }

  get widgetMapping(): WidgetMapping {
    return this._value.widgetMapping;
  }

  get widgetPositions(): { [string]: WidgetPosition } {
    return this._value.widgetPositions.toJS();
  }

  get staticMessageListId(): ?string {
    return this._value.staticMessageListId;
  }

  duplicate() {
    const widgetIdTranslation = {};
    const newWidgets = this.widgets.map((widget) => {
      const newWidget = widget.toBuilder().newId().build();

      widgetIdTranslation[widget.id] = newWidget.id;

      return newWidget;
    });
    const newWidgetTitles = Map(this.titles.get(TitleTypes.Widget, Map()).mapEntries(([key, value]) => [widgetIdTranslation[key], value]));
    const newTitles = this.titles
      .set(TitleTypes.Widget, newWidgetTitles)
      .updateIn([TitleTypes.Tab, 'title'], (value) => (value ? `${value} (Copy)` : value));
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
    const value: Object = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Map(value));
  }

  equals(other: any) {
    if (other === undefined) {
      return false;
    }

    if (!(other instanceof ViewState)) {
      return false;
    }

    return isDeepEqual(this.fields, other.fields)
      && isDeepEqual(this.titles, other.titles)
      && isDeepEqual(this.widgets, other.widgets)
      && isDeepEqual(this.widgetMapping, other.widgetMapping)
      && isDeepEqual(this.widgetPositions, other.widgetPositions)
      && isDeepEqual(this.formatting, other.formatting);
  }

  toJSON() {
    const { fields, formatting, titles, widgets, widgetMapping, widgetPositions } = this._value;

    return {
      selected_fields: fields,
      formatting,
      titles,
      widgets,
      widget_mapping: widgetMapping,
      positions: widgetPositions,
    };
  }

  static fromJSON(value: ViewStateJson): ViewState {
    const { selected_fields: selectedFields, titles, widgets, widget_mapping: widgetMapping, positions, formatting } = value;

    return ViewState.builder()
      .titles(fromJS(titles))
      .widgets(List(widgets.map((w) => Widget.fromJSON(w))))
      .widgetMapping(fromJS(widgetMapping))
      .fields(selectedFields)
      .widgetPositions(Map(positions).map(WidgetPosition.fromJSON))
      .formatting(formatting ? FormattingSettings.fromJSON(formatting) : FormattingSettings.empty())
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

  formatting(value: FormattingSettings) {
    return new Builder(this.value.set('formatting', value));
  }

  titles(value: (TitlesMap | { [TitleType]: { [string]: string } })): Builder {
    return new Builder(this.value.set('titles', fromJS(value)));
  }

  widgets(value: (List<Widget> | Array<Widget>)): Builder {
    return new Builder(this.value.set('widgets', List(value)));
  }

  widgetMapping(value: WidgetMapping): Builder {
    return new Builder(this.value.set('widgetMapping', value));
  }

  widgetPositions(value: (Map<string, WidgetPosition> | { [string]: WidgetPosition })): Builder {
    return new Builder(this.value.set('widgetPositions', Map(value)));
  }

  build(): ViewState {
    const { fields, formatting, titles, widgets, widgetMapping, widgetPositions } = this.value.toObject();

    return new ViewState(fields, titles, widgets, widgetMapping, widgetPositions, formatting);
  }
}
