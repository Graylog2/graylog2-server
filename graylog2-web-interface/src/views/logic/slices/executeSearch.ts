import * as Bluebird from 'bluebird';

import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import SearchResult from 'views/logic/SearchResult';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { runSearchJob, searchJobStatus } from 'views/stores/SearchJobs';
import type Search from 'views/logic/search/Search';
import type { SearchJobResult } from 'views/stores/SearchStore';
import type View from 'views/logic/views/View';

const trackJobStatus = (job: SearchJobResult, search: Search) => {
  return new Bluebird((resolve) => {
    if (job && job.execution.done) {
      return resolve(new SearchResult(job));
    }

    return resolve(Bluebird.delay(250)
      .then(() => searchJobStatus(job.id))
      .then((jobStatus) => trackJobStatus(jobStatus, search)));
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

  return runSearchJob(search, executionState).then((job) => trackJobStatus(job, search));
};

export default executeSearch;
