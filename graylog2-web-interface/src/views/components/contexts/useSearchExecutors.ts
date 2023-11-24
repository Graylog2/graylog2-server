import { useContext } from 'react';

import SearchExecutorsContext from 'views/components/contexts/SearchExecutorsContext';

const useSearchExecutors = () => useContext(SearchExecutorsContext);

export default useSearchExecutors;
