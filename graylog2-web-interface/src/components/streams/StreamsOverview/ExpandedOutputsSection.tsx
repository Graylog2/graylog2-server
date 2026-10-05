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

import type { Stream } from 'logic/streams/types';
import { Link, Pluralize, Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import type { Output } from 'hooks/useOutputs';
import useStreamOutputs from 'hooks/useStreamOutputs';
import usePermissions from 'hooks/usePermissions';

const editOutputUrl = (streamId: string, outputId: string) =>
  `${Routes.stream_view(streamId)}?segment=destinations&edit_output=${outputId}`;

type Props = {
  stream: Stream;
};

const OutputTitle = ({ streamId, output }: { streamId: string; output: Output }) => {
  const { isPermitted } = usePermissions();

  if (!isPermitted(`outputs:edit:${output.id}`)) {
    return <>{output.title}</>;
  }

  return <Link to={editOutputUrl(streamId, output.id)}>{output.title}</Link>;
};

const ExpandedOutputsSection = ({ stream }: Props) => {
  const { data, isInitialLoading } = useStreamOutputs(stream.id);

  if (isInitialLoading) {
    return <Spinner />;
  }

  const outputs = (data?.outputs ?? [])
    .slice()
    .sort((a, b) => naturalSort(a.title.toLowerCase(), b.title.toLowerCase()));

  return (
    <>
      <p>
        {outputs.length} connected <Pluralize value={outputs.length} singular="output" plural="outputs" />.
      </p>
      <ListGroup componentClass="ul">
        {outputs.map((output) => (
          <ListGroupItem key={output.id}>
            <OutputTitle streamId={stream.id} output={output} /> <small>({output.type})</small>
          </ListGroupItem>
        ))}
      </ListGroup>
    </>
  );
};

export default ExpandedOutputsSection;
