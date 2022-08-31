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
import React, { useEffect, useState } from 'react';

import { Alert } from 'components/bootstrap';
import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import StreamsStore from 'stores/streams/StreamsStore';
import useCurrentUser from 'hooks/useCurrentUser';

const StreamEditPage = ({ params, location }) => {
  const currentUser = useCurrentUser();
  const [stream, setStream] = useState();

  useEffect(() => {
    StreamsStore.get(params.streamId, (newStream) => {
      setStream(newStream);
    });
  }, [params.streamId]);

  const isLoading = !currentUser || !stream;

  if (isLoading) {
    return <Spinner />;
  }

  let content = (
    <StreamRulesEditor currentUser={currentUser}
                       streamId={params.streamId}
                       messageId={location.query.message_id}
                       index={location.query.index} />
  );

  if (stream.is_default) {
    content = (
      <div className="row content">
        <div className="col-md-12">
          <Alert bsStyle="danger">
            The default stream cannot be edited.
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <DocumentTitle title={`Rules of Stream ${stream.title}`}>
      <div>
        <PageHeader title={<span>Rules of Stream &raquo;{stream.title}&raquo;</span>}>
          <span>
            This screen is dedicated to an easy and comfortable creation and manipulation of stream rules. You can{' '}
            see the effect configured stream rules have on message matching here.
          </span>
        </PageHeader>

        {content}
      </div>
    </DocumentTitle>
  );
};

StreamEditPage.propTypes = {
  params: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
};

export default withParams(withLocation(StreamEditPage));
