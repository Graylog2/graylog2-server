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
import { useTranslation } from 'react-i18next';

import useOutputs from 'hooks/useOutputs';
import { IfPermitted, Section, Spinner } from 'components/common';
import type { Stream } from 'logic/streams/types';
import useStreamOutputs from 'hooks/useStreamOutputs';
import type { AvailableOutputSummary } from 'components/streams/useAvailableOutputTypes';
import useAvailableOutputTypes, {
  getOutputTypeDefinition,
  getRequestedOutputConfiguration,
} from 'components/streams/useAvailableOutputTypes';
import SectionCountLabel from 'components/streams/StreamDetails/SectionCountLabel';
import AddOutputButton from 'components/streams/StreamDetails/routing-destination/AddOutputButton';
import OutputsList from 'components/streams/StreamDetails/routing-destination/OutputsList';
import DestinationSwitch from 'components/streams/StreamDetails/routing-destination/DestinationSwitch';

type Props = {
  stream: Stream;
};

const DestinationOutputs = ({ stream }: Props) => {
  const { t } = useTranslation('streams');
  const { data, isInitialLoading } = useStreamOutputs(stream.id);
  const { data: outputs, isInitialLoading: isLoadingOutput } = useOutputs();
  const { data: availableOutputTypes, isInitialLoading: isLoadingOutputTypes } = useAvailableOutputTypes();

  const getTypeDefinition = (type: string, callback?: (available: AvailableOutputSummary) => void) => {
    const definition = getOutputTypeDefinition(availableOutputTypes, type);

    if (callback && definition) {
      callback(definition);
    }

    return getRequestedOutputConfiguration(availableOutputTypes, type);
  };

  if (isInitialLoading || isLoadingOutput || isLoadingOutputTypes) {
    return <Spinner />;
  }

  const hasAssignedOutput = data.outputs.length > 0;
  const title = hasAssignedOutput
    ? t('routing_destination.index_set.enabled')
    : t('routing_destination.index_set.disabled');

  const streamOutputIds = data.outputs.map((output) => output.id);
  const assignableOutputs = outputs.outputs
    .filter((output) => streamOutputIds.indexOf(output.id) === -1)
    .sort((output1, output2) => output1.title.localeCompare(output2.title));

  return (
    <Section
      title={t('routing_destination.outputs.title')}
      collapsible
      defaultClosed
      disableCollapseButton={!hasAssignedOutput}
      headerLeftSection={
        <>
          <DestinationSwitch
            aria-label={t('routing_destination.outputs.toggle_aria')}
            name="toggle-indexset"
            checked={hasAssignedOutput}
            disabled
            onChange={(e) => e.preventDefault()}
            label={title}
          />
          <SectionCountLabel>
            {t('routing_destination.outputs.count', { count: data.outputs.length })}
          </SectionCountLabel>
        </>
      }
      actions={
        <IfPermitted permissions="stream_outputs:create">
          <AddOutputButton
            stream={stream}
            availableOutputTypes={availableOutputTypes}
            assignableOutputs={assignableOutputs}
            getTypeDefinition={getTypeDefinition}
          />
        </IfPermitted>
      }>
      <OutputsList
        streamId={stream.id}
        outputs={data.outputs}
        isLoadingOutputTypes={isLoadingOutputTypes}
        getTypeDefinition={getTypeDefinition}
      />
    </Section>
  );
};

export default DestinationOutputs;
