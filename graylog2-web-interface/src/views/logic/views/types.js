// @flow strict
import * as Immutable from 'immutable';

type SearchTypeIds = Immutable.Set<string>;
export type WidgetId = string;

export type WidgetMapping = Immutable.Map<WidgetId, SearchTypeIds>;
