// @flow strict
import * as Immutable from 'immutable';
import HighlightingRule from './highlighting/HighlightingRule';
import type { HighlightingRuleJSON } from './highlighting/HighlightingRule';

type HighlightingRules = Array<HighlightingRule>;

type InternalState = {
  highlighting: HighlightingRules,
};

export type FormattingSettingsJSON = {
  highlighting: Array<HighlightingRuleJSON>,
};

export default class FormattingSettings {
  _value: InternalState;

  constructor(highlighting: HighlightingRules = []) {
    this._value = { highlighting };
  }

  get highlighting(): HighlightingRules {
    return this._value.highlighting;
  }

  toBuilder() {
    const { highlighting } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ highlighting }));
  }

  static create(highlighting: HighlightingRules) {
    return new FormattingSettings(highlighting);
  }

  static empty() {
    // eslint-disable-next-line no-use-before-define
    return new Builder().build();
  }

  toJSON() {
    const { highlighting } = this._value;

    return {
      highlighting,
    };
  }

  static fromJSON(value: FormattingSettingsJSON) {
    const { highlighting = [] } = value;
    return FormattingSettings.create(highlighting.map((rule) => HighlightingRule.fromJSON(rule)));
  }
}

class Builder {
  value: Immutable.Map<string, any>;

  constructor(value: Immutable.Map<string, any> = Immutable.Map()) {
    this.value = value;
  }

  highlighting(value: HighlightingRules) {
    return new Builder(this.value.set('highlighting', value));
  }

  build() {
    const { highlighting } = this.value.toObject();
    return new FormattingSettings(highlighting);
  }
}
