// @flow strict
import Widget from './widgets/Widget';

type WidgetInternalState = {
  widget: Widget,
};

export class ActionContext {
  static empty() {
    return new ActionContext();
  }
}

export class WidgetContext extends ActionContext {
  _value: WidgetInternalState;
  constructor(widget: Widget) {
    super();
    this._value = { widget };
  }

  get widget() {
    return this._value.widget;
  }

  static create(widget: Widget) {
    return new WidgetContext(widget);
  }
}
