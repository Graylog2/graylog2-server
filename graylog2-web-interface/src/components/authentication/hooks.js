// @flow strict
/* eslint-disable import/prefer-default-export */
import { useState, useEffect } from 'react';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';

export const useActiveBackend = () => {
  const [activeBackend, setActiveBackend] = useState();
  const [finishedLoading, setFinishedLoading] = useState(false);

  useEffect(() => {
    AuthenticationDomain.loadActive().then((backend) => {
      setFinishedLoading(true);

      if (backend) {
        setActiveBackend(backend);
      }
    });
  }, []);

  return { finishedLoading, activeBackend };
};
