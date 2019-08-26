import React from 'react';

import Routes from '../common/Routes';

const Services = () => {
  return (
    <div>
        List of Services.

      <ul><li><a href={Routes.INTEGRATIONS.AWS.CLOUDWATCH.index}>CloudWatch</a></li></ul>
    </div>
  );
};

export default Services;
