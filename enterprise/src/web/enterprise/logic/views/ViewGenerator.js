import View from './View';
import Search from '../search/Search';
import QueryGenerator from '../queries/QueryGenerator';
import ViewStateGenerator from './ViewStateGenerator';

export default () => {
  const query = QueryGenerator();
  const search = Search.create().toBuilder().queries([query]).build();
  const viewState = ViewStateGenerator();
  return View.create()
    .toBuilder()
    .state({ [query.id]: viewState })
    .search(search)
    .build();
};