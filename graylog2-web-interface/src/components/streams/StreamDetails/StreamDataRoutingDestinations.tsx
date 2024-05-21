import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

const StreamDataRoutingDestinations = () => {
  const StreamDataWarehouseComponent = PluginStore.exports('dataWarehouse')?.[0]?.StreamDataWarehouse;

  return (
    <StreamDataWarehouseComponent />
  );
};

export default StreamDataRoutingDestinations;
