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
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { Button, Col, Panel, Row } from 'components/bootstrap';
import { Icon } from 'components/common';
import LoaderTabs from 'components/messageloaders/LoaderTabs';
import MatchingTypeSwitcher from 'components/streams/MatchingTypeSwitcher';
import StreamRuleList from 'components/streamrules/StreamRuleList';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import Spinner from 'components/common/Spinner';
import type { MatchData } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';

import useStream from '../streams/hooks/useStream';

const StreamAlertHeader = styled(Panel.Heading)`
  font-weight: bold;
`;

const MatchIcon = styled(Icon)<{ $empty?: boolean, $matches?: boolean }>(({ theme, $empty, $matches }) => {
  const matchColor = $matches ? theme.colors.variant.success : theme.colors.variant.danger;

  return css`
      color: ${$empty ? theme.colors.variant.info : matchColor};
      margin-right: 3px;
`;
});

const StyledSpinner = styled(Spinner)`
  margin-left: 10px;
`;

const getListClassName = (matchData) => (matchData.matches ? 'success' : 'danger');

type Props = {
  streamId: string,
  messageId?: string | undefined
  index?: string
}

const StreamRulesEditor = ({ streamId, messageId = '', index = '' }: Props) => {
  const [showStreamRuleForm, setShowStreamRuleForm] = useState(false);
  const [message, setMessage] = useState<{ [fieldName: string]: unknown } | undefined>();
  const [matchData, setMatchData] = useState<MatchData | undefined>();
  const { data: stream, refetch } = useStream(streamId);

  useEffect(() => {
    const refetchStrems = () => refetch();
    StreamsStore.onChange(refetchStrems);
    StreamRulesStore.onChange(refetchStrems);

    return () => {
      StreamsStore.unregister(refetchStrems);
      StreamRulesStore.unregister(refetchStrems);
    };
  }, [refetch]);

  const onMessageLoaded = (newMessage) => {
    setMessage(newMessage);

    if (message !== undefined) {
      StreamsStore.testMatch(streamId, { message: message.fields }, (resultData) => {
        setMatchData(resultData);
      });
    } else {
      setMatchData(undefined);
    }
  };

  const _onStreamRuleFormSubmit = (_streamRuleId: string, data) => StreamRulesStore.create(streamId, data, () => {});

  const _onAddStreamRule = (event) => {
    event.preventDefault();
    setShowStreamRuleForm(true);
  };

  const styles = (matchData ? getListClassName(matchData) : 'info');

  if (!stream) {
    return (
      <Row className="content">
        <StyledSpinner />
      </Row>
    );
  }

  return (
    <Row className="content">
      <Col md={12} className="streamrule-sample-message">
        <h2>1. Load a message to test rules</h2>

        <div className="stream-loader">
          <LoaderTabs messageId={messageId}
                      index={index}
                      onMessageLoaded={onMessageLoaded} />
        </div>

        <hr />

        <div className="buttons pull-right">
          <Button bsStyle="success"
                  className="show-stream-rule"
                  onClick={_onAddStreamRule}>
            Add stream rule
          </Button>
          {showStreamRuleForm && (
            <StreamRuleModal title="New Stream Rule"
                             onClose={() => setShowStreamRuleForm(false)}
                             submitButtonText="Create Rule"
                             submitLoadingText="Creating Rule..."
                             onSubmit={_onStreamRuleFormSubmit} />
          )}
        </div>

        <h2>2. Manage stream rules</h2>

        <MatchingTypeSwitcher stream={stream} onChange={refetch} />
        <Panel bsStyle={styles}>
          <StreamAlertHeader>
            {matchData?.matches && (
              <>
                <MatchIcon $matches name="check" /> This message would be routed to this stream!
              </>
            )}

            {(matchData && !matchData.matches) && (
              <>
                <MatchIcon name="close" /> This message would not be routed to this stream.
              </>
            )}

            {!matchData && (
              <>
                <MatchIcon $empty name="error" /> Please load a message in Step 1 above to check if it would match against these rules.
              </>
            )}
          </StreamAlertHeader>
          <StreamRuleList stream={stream}
                          matchData={matchData} />
        </Panel>

        <p>
          <LinkContainer to={Routes.STREAMS}>
            <Button bsStyle="success">I&apos;m done!</Button>
          </LinkContainer>
        </p>
      </Col>
    </Row>
  );
};

export default StreamRulesEditor;
