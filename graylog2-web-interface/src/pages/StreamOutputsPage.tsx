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
import React from 'react';
import { useParams } from 'react-router-dom';

import { Link } from 'components/common/router';
import { Col } from 'components/bootstrap';
import { ContentHeadRow, DocumentTitle, Spinner } from 'components/common';
import OutputsComponent from 'components/outputs/OutputsComponent';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import useStream from 'components/streams/hooks/useStream';

const StreamOutputsPage = () => {
  const currentUser = useCurrentUser();
  const { streamId } = useParams();
  const { data: stream } = useStream(streamId);

  if (!stream) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={`Outputs for Stream ${stream.title}`}>
      <div>
        <ContentHeadRow className="content">
          <Col md={10}>
            <h1>
              Outputs for Stream &raquo;{stream.title}&laquo;
            </h1>

            <p className="description">
              Graylog nodes can forward messages of streams via outputs. Launch or terminate as many outputs as you want here.
              You can also reuse outputs that are already running for other streams.

              A global view of all configured outputs is available <Link to={Routes.SYSTEM.OUTPUTS}>here</Link>.
              You can find output plugins on <a href="https://marketplace.graylog.org/" rel="noopener noreferrer" target="_blank">the Graylog Marketplace</a>.
              <br />

              <i>Removing</i> an output removes it from this stream but it will still be in the list of available outputs.
              Deleting an output <i>globally</i> will remove it from this and all other streams and terminate it.
              You can see all defined outputs in details at the {' '} <Link to={Routes.SYSTEM.OUTPUTS}>global output list</Link>.
            </p>
          </Col>
        </ContentHeadRow>
        <OutputsComponent streamId={stream.id} permissions={currentUser.permissions} />
      </div>
    </DocumentTitle>
  );
};

export default StreamOutputsPage;
