import * as React from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

export const UnlicensedText = () => (
  <p>
    Only paid licenses allow you to use graylog enterprise support. You may still use this file in other ways.
  </p>
);

const ClusterSupportBundleInfo = () => {
  const pluginLogger = usePluginEntities('logger');
  const InfoComponent = pluginLogger[0]?.EnterpriseSupportBundleInfo || UnlicensedText;

  return (
    <p className="description">
      Create a zip file which contains useful debugging information from your Graylog cluster.<br />
      Graylog Enterprise customers can attach bundles to their support ticket, which will help the Graylog technical
      support team
      with analyzing and diagnosing issues.<br />
      <strong>Please examine the bundle before sending it to Graylog.
        It might contain sensitive data like IP addresses, hostnames or even passwords!
      </strong>
      {InfoComponent && (<InfoComponent />)}
    </p>
  );
};

export default ClusterSupportBundleInfo;
