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
