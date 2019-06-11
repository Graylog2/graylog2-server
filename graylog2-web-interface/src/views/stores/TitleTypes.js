// @flow strict
import { Map } from 'immutable';

export type TitleType = 'tab' | 'widget';
export type TitlesMap = Map<TitleType, Map<string, string>>;

const TitleTypes: { [string]: TitleType } = {
  Tab: 'tab',
  Widget: 'widget',
};

export default TitleTypes;
