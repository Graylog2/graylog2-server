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

import { Section, Spinner } from 'components/common';
import type { Stream } from 'stores/streams/StreamsStore';
import useStreamOutputs from 'hooks/useStreamOutputs';

import OutputsList from './OutputsList';

type Props = {
  stream: Stream
};

const OutputsDestination = ({ stream }: Props) => {
  const { data, isInitialLoading } = useStreamOutputs(stream.id);

  if (isInitialLoading) {
    return <Spinner />;
  }

  return (
    <Section title="Outputs">
      <OutputsList streamId={stream.id} outputs={data.outputs} />
    </Section>
  );
};

export default OutputsDestination;
