import Bluebird from 'bluebird';

import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import type { SearchJobType } from 'views/stores/SearchJobs';
import { runSearchJob, searchJobStatus } from 'views/stores/SearchJobs';
import type View from 'views/logic/views/View';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import SearchResult from 'views/logic/SearchResult';

const trackJobStatus = (job: SearchJobType): Promise<SearchJobType> => {
  return new Bluebird((resolve) => {
    if (job?.execution?.done) {
      return resolve(job);
    }

    return resolve(Bluebird.delay(250)
      .then(() => searchJobStatus(job.id))
      .then((jobStatus) => trackJobStatus(jobStatus)));
  });
};

const executeSearch = (
  view: View,
  widgetsToSearch: string[],
  executionStateParam: SearchExecutionState,
): Promise<SearchExecutionResult> => {
  const { widgetMapping, search } = view;

  let executionStateBuilder = executionStateParam.toBuilder();

  if (widgetsToSearch) {
    const { globalOverride = GlobalOverride.empty() } = executionStateParam;
    const keepSearchTypes = widgetsToSearch.map((widgetId) => widgetMapping.get(widgetId))
      .reduce((acc, searchTypeSet) => [...acc, ...searchTypeSet.toArray()], globalOverride.keepSearchTypes || []);
    const newGlobalOverride = globalOverride.toBuilder().keepSearchTypes(keepSearchTypes).build();
    executionStateBuilder = executionStateBuilder.globalOverride(newGlobalOverride);
  }

  const executionState = executionStateBuilder.build();

  return runSearchJob(search, executionState)
    .then((job) => trackJobStatus(job))
    .then((result) => ({ widgetMapping, result: new SearchResult(result) }));
};

export default executeSearch;
