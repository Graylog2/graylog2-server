// @flow strict
import { Map } from 'immutable';

type State = {
  col: number,
  row: number,
  height: number,
  width: number,
};

export type WidgetPositionJSON = {
  col: number | 'Infinity',
  row: number | 'Infinity',
  height: number | 'Infinity',
  width: number | 'Infinity',
};

export default class WidgetPosition {
  _value: State;

  constructor(col: number, row: number, height: number, width: number) {
    this._value = { col, row, height, width };
  }

  static fromJSON(value: WidgetPositionJSON) {
    const { col, row, height, width } = value;
    const newCol = col === 'Infinity' ? Infinity : col;
    const newRow = row === 'Infinity' ? Infinity : row;
    const newHeight = height === 'Infinity' ? Infinity : height;
    const newWidth = width === 'Infinity' ? Infinity : width;

    return new WidgetPosition(newCol, newRow, newHeight, newWidth);
  }

  get col(): number {
    return this._value.col;
  }

  get row(): number {
    return this._value.row;
  }

  get height(): number {
    return this._value.height;
  }

  get width(): number {
    return this._value.width;
  }

  toJSON(): WidgetPositionJSON {
    const { col, row, height, width } = this._value;

    const newCol = col === Infinity ? 'Infinity' : col;
    const newRow = row === Infinity ? 'Infinity' : row;
    const newHeight = height === Infinity ? 'Infinity' : height;
    const newWidth = width === Infinity ? 'Infinity' : width;

    return {
      col: newCol,
      row: newRow,
      height: newHeight,
      width: newWidth,
    };
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map(this._value));
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

class Builder {
  value: Map<string, any>;

  constructor(value: Map<string, any> = Map()) {
    this.value = value;
  }

  col(value: number) {
    this.value = this.value.set('col', value);
    return this;
  }

  row(value: number) {
    this.value = this.value.set('row', value);
    return this;
  }

  height(value: number) {
    this.value = this.value.set('height', value);
    return this;
  }

  width(value: number) {
    this.value = this.value.set('width', value);
    return this;
  }

  build(): WidgetPosition {
    const { col, row, height, width } = this.value.toObject();
    return new WidgetPosition(col, row, height, width);
  }
}
