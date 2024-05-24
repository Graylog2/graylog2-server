import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Section } from 'components/common';
import type { Stream } from 'stores/streams/StreamsStore';

type Props =  { stream: Stream }

const StreamDataRoutingDestinations = ({ stream } : Props) => {
  const StreamDataWarehouseComponent = PluginStore.exports('dataWarehouse')?.[0]?.StreamDataWarehouse;

  return (
    <>
      <Section title='Index Set'>
        <dl>
          <dd>IndexSet Name: Default index set</dd>
        </dl>

      </Section>
      <StreamDataWarehouseComponent />
    </>
  );
};

export default StreamDataRoutingDestinations;
