export default class WidgetPosition {
  constructor(col, row, height, width) {
    this._value = { col, row, height, width };
  }

  static fromJSON(value) {
    const { col, row, height, width } = value;
    const newCol = col === 'Infinity' ? Infinity : col;
    const newRow = row === 'Infinity' ? Infinity : row;
    const newHeight = height === 'Infinity' ? Infinity : height;
    const newWidth = width === 'Infinity' ? Infinity : width;

    return new WidgetPosition(newCol, newRow, newHeight, newWidth);
  }

  get col() {
    return this._value.col;
  }

  get row() {
    return this._value.row;
  }

  get height() {
    return this._value.height;
  }

  get width() {
    return this._value.width;
  }

  toJSON() {
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
}
