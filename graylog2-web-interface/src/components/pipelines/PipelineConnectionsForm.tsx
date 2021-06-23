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
import PropTypes from 'prop-types';
import React, { useMemo, useRef, useState } from 'react';
import naturalSort from 'javascript-natural-sort';

import { Link } from 'components/graylog/router';
import { ControlLabel, FormGroup, HelpBlock, Button } from 'components/graylog';
import { SelectableList } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Routes from 'routing/Routes';
import { PipelineType } from 'stores/pipelines/PipelinesStore';
import { Stream } from 'stores/streams/StreamsStore';
import { PipelineConnectionsType } from 'stores/pipelines/PipelineConnectionsStore';

type Props = {
  pipeline: PipelineType,
  connections: PipelineConnectionsType[],
  streams: Stream[],
  save: (newConnection, callback) => void,
};

type FormattedStream = {
  value: string,
  label: string,
}

const formatStreams = (streams: Stream[]): FormattedStream[] => {
  return streams
    .map((s) => ({ value: s.id, label: s.title }))
    .sort((s1, s2) => naturalSort(s1.label, s2.label));
};

const PipelineConnectionsForm = ({ pipeline, connections, streams, save }: Props) => {
  const modalRef = useRef<BootstrapModalForm>();

  const initialStreamConnections = useMemo(() => {
    return connections
      .filter((c) => c.pipeline_ids && c.pipeline_ids.includes(pipeline.id)) // Get connections for this pipeline
      .filter((c) => streams.some((s) => s.id === c.stream_id)) // Filter out deleted streams
      .map((c) => streams.find((s) => s.id === c.stream_id));
  }, [pipeline, connections, streams]);

  const [connectedStreams, setConnectedStreams] = useState<FormattedStream[]>(formatStreams(initialStreamConnections));

  const notConnectedStreams = useMemo(() => {
    return streams.filter((s) => !connectedStreams.some((cs) => cs.value.toLowerCase() === s.id.toLowerCase()));
  }, [streams, connectedStreams]);

  const _openModal = () => {
    if (modalRef.current) {
      modalRef.current.open();
    }
  };

  const _onStreamsChange = (newStreams) => {
    setConnectedStreams(newStreams.sort((s1, s2) => naturalSort(s1.label, s2.label)));
  };

  const _closeModal = () => {
    if (modalRef.current) {
      modalRef.current.close();
    }
  };

  const _save = () => {
    const streamIds = connectedStreams.map((cs) => cs.value);
    const newConnection = {
      pipeline: pipeline.id,
      streams: streamIds,
    };

    save(newConnection, _closeModal);
  };

  const streamsHelp = (
    <span>
      Select the streams you want to connect to this pipeline, or create one in the{' '}
      <Link to={Routes.STREAMS}>Streams page</Link>.
    </span>
  );

  return (
    <span>
      <Button onClick={_openModal} bsStyle="info">
        <span>Edit connections</span>
      </Button>
      <BootstrapModalForm ref={modalRef}
                          title={<span>Edit connections for <em>{pipeline.title}</em></span>}
                          onSubmitForm={_save}
                          submitButtonText="Save">
        <fieldset>
          <FormGroup id="streamsConnections">
            <ControlLabel>Streams</ControlLabel>
            <SelectableList options={formatStreams(notConnectedStreams)}
                            onChange={_onStreamsChange}
                            selectedOptionsType="object"
                            selectedOptions={connectedStreams} />
            <HelpBlock>{streamsHelp}</HelpBlock>
          </FormGroup>
        </fieldset>
      </BootstrapModalForm>
    </span>
  );
};

PipelineConnectionsForm.propTypes = {
  pipeline: PropTypes.object.isRequired,
  connections: PropTypes.array.isRequired,
  streams: PropTypes.array.isRequired,
  save: PropTypes.func.isRequired,
};

export default PipelineConnectionsForm;
