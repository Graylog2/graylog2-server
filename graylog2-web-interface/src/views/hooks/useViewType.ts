import { useContext } from 'react';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

const useViewType = () => useContext(ViewTypeContext);

export default useViewType;
