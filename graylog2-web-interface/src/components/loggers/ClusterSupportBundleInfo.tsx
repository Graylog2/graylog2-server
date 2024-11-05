/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/// <reference path="./types.ts" />
import * as React from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

export const UnlicensedText = () => (
  <>
    <strong>Please examine the bundle content before sharing it.
      It might contain sensitive data like IP addresses, hostnames or even passwords!
    </strong><br />
    Only paid license users are entitled to use Graylog enterprise support.
    You may however use this file for other means of debugging.
  </>
);

const ClusterSupportBundleInfo = () => {
  const pluginLogger = usePluginEntities('supportBundle');
  const InfoComponent = pluginLogger[0]?.EnterpriseSupportBundleInfo || UnlicensedText;

  return (
    <p className="description">
      Create a zip file which contains useful debugging information from your Graylog cluster.<br />
      Graylog Enterprise customers can attach bundles to their support ticket, which will help the Graylog technical
      support team with analyzing and diagnosing issues.<br />
      <br />
      {InfoComponent && (<InfoComponent />)}
    </p>
  );
};

export default ClusterSupportBundleInfo;
