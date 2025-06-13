import WidgetConfig from 'views/logic/widgets/WidgetConfig';

export type TextWidgetConfigJSON = {
  text: string;
};
export default class TextWidgetConfig extends WidgetConfig {
  private readonly _text: string;

  constructor(text: string) {
    super();
    this._text = text;
  }

  get text() {
    return this._text;
  }

  toJSON() {
    return {
      text: this._text,
    };
  }

  static empty() {
    return new TextWidgetConfig('');
  }

  static fromJSON(value: TextWidgetConfigJSON) {
    return new TextWidgetConfig(value.text);
  }

  equals(other: any): boolean {
    return other instanceof TextWidgetConfig && this._text === other.text;
  }

  // eslint-disable-next-line class-methods-use-this
  equalsForSearch(_other: any): boolean {
    // Should never trigger a new search
    return true;
  }
}
