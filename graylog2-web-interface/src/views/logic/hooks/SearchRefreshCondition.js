// @flow strict
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import View from 'views/logic/views/View';

export type SearchRefreshConditionArguments = { view: View, searchMetadata: SearchMetadata, executionState: SearchExecutionState };
export type SearchRefreshCondition = (SearchRefreshConditionArguments) => boolean;
