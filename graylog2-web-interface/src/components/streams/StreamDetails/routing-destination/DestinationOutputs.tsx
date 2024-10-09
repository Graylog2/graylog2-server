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
import * as React from 'react';

import useOutputs from 'hooks/useOutputs';
import { Section, Spinner } from 'components/common';
import type { Stream } from 'stores/streams/StreamsStore';
import useStreamOutputs from 'hooks/useStreamOutputs';
import type { AvailableOutputSummary } from 'components/streams/useAvailableOutputTypes';
import useAvailableOutputTypes from 'components/streams/useAvailableOutputTypes';
import SectionCountLabel from 'components/streams/StreamDetails/SectionCountLabel';
import AddOutputButton from 'components/streams/StreamDetails/routing-destination/AddOutputButton';
import OutputsList from 'components/streams/StreamDetails/routing-destination/OutputsList';
import DestinationSwitch from 'components/streams/StreamDetails/routing-destination/DestinationSwitch';

type Props = {
  stream: Stream
};

const DestinationOutputs = ({ stream }: Props) => {
  const { data, isInitialLoading } = useStreamOutputs(stream.id);
  const { data: outputs, isInitialLoading: isLoadingOutput } = useOutputs();
  const { data: availableOutputTypes, isInitialLoading: isLoadingOutputTypes } = useAvailableOutputTypes();

  const getTypeDefinition = (type: string, callback?: (available: AvailableOutputSummary) => void) => {
    const definitition = availableOutputTypes.types[type];

    if (callback && definitition) {
      callback(definitition);
    }

    return definitition?.requested_configuration;
  };

  if (isInitialLoading || isLoadingOutput || isLoadingOutputTypes) {
    return <Spinner />;
  }

  const hasAssignedOutput = data.outputs.length > 0;
  const title = hasAssignedOutput ? 'Enabled' : 'Disabled';

  const streamOutputIds = data.outputs.map((output) => output.id);
  const assignableOutputs = outputs.outputs
    .filter((output) => streamOutputIds.indexOf(output.id) === -1)
    .sort((output1, output2) => output1.title.localeCompare(output2.title));

  return (
    <Section title="Outputs"
             collapsible
             defaultClosed
             disableCollapseButton={!hasAssignedOutput}
             headerLeftSection={(
               <>
                 <DestinationSwitch aria-label="Toggle Output"
                                    name="toggle-indexset"
                                    checked={hasAssignedOutput}
                                    disabled
                                    onChange={(e) => e.preventDefault()}
                                    label={title} />
                 <SectionCountLabel>OUTPUTS {data.outputs.length}</SectionCountLabel>
               </>
             )}
             actions={(
               <AddOutputButton stream={stream}
                                availableOutputTypes={availableOutputTypes.types}
                                assignableOutputs={assignableOutputs}
                                getTypeDefinition={getTypeDefinition} />
            )}>
      <OutputsList streamId={stream.id}
                   outputs={data.outputs}
                   isLoadingOutputTypes={isLoadingOutputTypes}
                   getTypeDefinition={getTypeDefinition} />
    </Section>
  );
};

export default DestinationOutputs;
