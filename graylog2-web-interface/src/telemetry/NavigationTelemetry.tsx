import React, { useEffect } from 'react';

import useLocation from 'routing/useLocation';
import useSendTelemetry from 'telemetry/useSendTelemetry';

const NavigationTelemetry = () => {
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    if (location.pathname) {
      sendTelemetry('$pageview', {});
    }
  }, [location.pathname, sendTelemetry]);

  return (
    <div />
  );
};

export default NavigationTelemetry;
