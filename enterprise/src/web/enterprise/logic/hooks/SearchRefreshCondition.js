// @flow strict
import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';
import SearchMetadata from 'enterprise/logic/search/SearchMetadata';
import View from 'enterprise/logic/views/View';

export type SearchRefreshConditionArguments = { view: View, searchMetadata: SearchMetadata, executionState: SearchExecutionState };
export type SearchRefreshCondition = (SearchRefreshConditionArguments) => boolean;

