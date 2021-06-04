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
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { Row, Col } from 'components/graylog';
import AppConfig from 'util/AppConfig';
import InputDropdown from 'components/inputs/InputDropdown';
import UserNotification from 'util/UserNotification';
import StoreProvider from 'injection/StoreProvider';
import type { Message } from 'views/components/messagelist/Types';

import type { Input } from './Types';

const UniversalSearchStore = StoreProvider.getStore('UniversalSearch');
const isCloud = AppConfig.isCloud();

const StyledContainer = styled.div`
  margin-top: 5px;
`;

const useForwarderMessageLoaders = () => {
  const messageLoaders = PluginStore.exports('forwarder')?.[0]?.messageLoaders;

  return messageLoaders || { ForwarderInputDropdown: undefined };
};

type ServerInputSelectProps = {
  inputs: Immutable.Map<string, Input>,
  onChange: (inputId: string) => void,
  selectedInputId: string | undefined,
  isLoading: boolean,
};

const ServerInputSelect = ({ selectedInputId, inputs, onChange, isLoading }: ServerInputSelectProps) => {
  return (
    <fieldset>
      {selectedInputId
        ? 'Click on "Load Message" to load the most recent message received by this input within the last hour.'
        : 'Select an Input from the list below and click "Load Message" to load the most recent message received by this input within the last hour.'}
      <InputDropdown inputs={inputs}
                     preselectedInputId={selectedInputId}
                     onLoadMessage={onChange}
                     title={isLoading ? 'Loading message...' : 'Load Message'}
                     disabled={isLoading} />
    </fieldset>
  );
};

type ForwaderInputSelectProps = {
  onChange: (inputId: string) => void,
  selectedInputId: string | undefined,
  isLoading: boolean,
};

const ForwarderInputSelect = ({ selectedInputId, onChange, isLoading }: ForwaderInputSelectProps) => {
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  return (
    <Row>
      <Col md={8}>
        <p className="description">
          Select an Input profile from the list below then select an then select an Input and click
          on &quot;Load message&quot; to load most recent message received by this input within the last hour.
        </p>
        <ForwarderInputDropdown onLoadMessage={onChange}
                                title={isLoading ? 'Loading message...' : 'Load Message'}
                                preselectedInputId={selectedInputId}
                                loadButtonDisabled={isLoading} />
      </Col>
    </Row>
  );
};

type Props = {
  inputs: Immutable.Map<string, Input>,
  onMessageLoaded: (message?: Message) => void,
  selectedInputId?: string,
};

const RecentMessageLoader = ({ inputs, onMessageLoaded, selectedInputId }: Props) => {
  const [loading, setLoading] = useState<boolean>(false);
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  const onClick = (inputId: string) => {
    const input = inputs && inputs.get(inputId);

    if (!isCloud && !input) {
      UserNotification.error(`Invalid input selected: ${inputId}`,
        `Could not load message from invalid Input ${inputId}`);
    }

    setLoading(true);
    const promise = UniversalSearchStore.search('relative', `gl2_source_input:${inputId} OR gl2_source_radio_input:${inputId}`,
      { range: 3600 }, undefined, 1, undefined, undefined, undefined, false);

    promise.then((response) => {
      if (response.total_results > 0) {
        onMessageLoaded(response.messages[0]);
      } else {
        UserNotification.error('Input did not return a recent message.');
        onMessageLoaded(undefined);
      }
    });

    promise.finally(() => setLoading(false));
  };

  return (
    <StyledContainer>
      {ForwarderInputDropdown
        ? (
          <ForwarderInputSelect selectedInputId={selectedInputId} onChange={onClick} isLoading={loading} />
        ) : (
          <ServerInputSelect selectedInputId={selectedInputId} inputs={inputs} onChange={onClick} isLoading={loading} />
        )}
    </StyledContainer>
  );
};

RecentMessageLoader.propTypes = {
  inputs: PropTypes.object,
  onMessageLoaded: PropTypes.func.isRequired,
  selectedInputId: PropTypes.string,
};

RecentMessageLoader.defaultProps = {
  inputs: Immutable.Map(),
  selectedInputId: undefined,
};

export default RecentMessageLoader;
