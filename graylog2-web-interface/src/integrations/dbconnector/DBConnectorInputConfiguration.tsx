import { useEffect } from 'react';

import useHistory from 'routing/useHistory';

import DBConnectorRoutes from './Routes';

const DBConnectorInputConfiguration = () => {
  const history = useHistory();

  useEffect(() => {
    const url = DBConnectorRoutes.INTEGRATIONS.DBConnector.ACTIVITYAPI.index;
    history.push(url);
  }, [history]);

  return null;
};

export default DBConnectorInputConfiguration;