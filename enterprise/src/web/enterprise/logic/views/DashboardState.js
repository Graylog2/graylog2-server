import Immutable from 'immutable';
import DashboardWidget from './DashboardWidget';

export default class DashboardState {
  constructor(widgets, positions) {
    this._value = {
      widgets: Immutable.fromJS(widgets),
      positions: Immutable.fromJS(positions),
    };
  }

  static create() {
    return new DashboardState({}, {});
  }

  get widgets() {
    return this._value.widgets;
  }

  get positions() {
    return this._value.positions;
  }

  toBuilder() {
    const { widgets, positions } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ widgets, positions }));
  }

  toJSON() {
    const { widgets, positions } = this._value;
    return {
      widgets,
      positions,
    };
  }

  static fromJSON(value) {
    const { widgets, positions } = value;
    const newWidgets = Immutable.Map(widgets).map(w => DashboardWidget.fromJSON(w));
    return new DashboardState(newWidgets, positions);
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  positions(value) {
    return new Builder(this.value.set('positions', value));
  }

  widgets(value) {
    return new Builder(this.value.set('widgets', value));
  }

  build() {
    const { widgets, positions } = this.value.toObject();
    return new DashboardState(widgets, positions);
  }
}