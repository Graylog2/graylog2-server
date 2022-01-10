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
import PropTypes from 'prop-types';
import type * as Immutable from 'immutable';
import styled from 'styled-components';

import { Row, Col, Input } from 'components/bootstrap';
import AppConfig from 'util/AppConfig';
import InputDropdown from 'components/inputs/InputDropdown';
import UserNotification from 'util/UserNotification';
import type { Message } from 'views/components/messagelist/Types';
import useForwarderMessageLoaders from 'components/messageloaders/useForwarderMessageLoaders';
import { UniversalSearchStore } from 'stores/search/UniversalSearchStore';

import type { Input as InputType } from './Types';

const LoaderContainer = styled.div`
  margin-top: 5px;
`;

const Description = styled.p`
  margin-bottom: 0.5em;
`;

const StyledSelect = styled(Input)`
  width: 200px;
`;

type ServerInputSelectProps = {
  inputs: Immutable.Map<string, InputType>,
  onChange: (inputId: string) => void,
  selectedInputId: string | undefined,
  isLoading: boolean,
};

const ServerInputSelect = ({ selectedInputId, inputs, onChange, isLoading }: ServerInputSelectProps) => {
  return (
    <fieldset>
      <Description>
        {selectedInputId
          ? 'Click on "Load Message" to load the most recent message received by this input within the last hour.'
          : 'Select an Input from the list below and click "Load Message" to load the most recent message received by this input within the last hour.'}
      </Description>
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
    <fieldset>
      <Description>
        Select an Input profile from the list below then select an then select an Input and click
        on &quot;Load message&quot; to load most recent message received by this input within the last hour.
      </Description>
      <Row>
        <Col md={8}>
          <ForwarderInputDropdown onLoadMessage={onChange}
                                  title={isLoading ? 'Loading message...' : 'Load Message'}
                                  preselectedInputId={selectedInputId}
                                  loadButtonDisabled={isLoading} />
        </Col>
      </Row>
    </fieldset>
  );
};

type Props = {
  inputs?: Immutable.Map<string, InputType>,
  onMessageLoaded: (message?: Message) => void,
  selectedInputId?: string,
};

const RecentMessageLoader = ({ inputs, onMessageLoaded, selectedInputId }: Props) => {
  const [loading, setLoading] = useState<boolean>(false);
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  const [selectedInputType, setSelectedInputType] = useState<'forwarder' | 'server'>(ForwarderInputDropdown ? undefined : 'server');
  const isCloud = AppConfig.isCloud();

  useEffect(() => {
    if (selectedInputId && inputs) {
      setSelectedInputType(inputs?.get(selectedInputId) ? 'server' : 'forwarder');
    }
  }, [inputs, selectedInputId]);

  const onClick = (inputId: string) => {
    const input = inputs && inputs.get(inputId);

    if (selectedInputType === 'server' && !input) {
      UserNotification.error(`Invalid input selected: ${inputId}`,
        `Could not load message from invalid Input ${inputId}`);

      return;
    }

    setLoading(true);
    const promise = UniversalSearchStore.search('relative', `gl2_source_input:${inputId} OR gl2_source_radio_input:${inputId}`, { range: 3600 }, undefined, 1, undefined, undefined, undefined, false);

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

  if (isCloud) {
    return (
      <LoaderContainer>
        <ForwarderInputSelect selectedInputId={selectedInputId} onChange={onClick} isLoading={loading} />
      </LoaderContainer>
    );
  }

  return (
    <LoaderContainer>
      {ForwarderInputDropdown
        ? (
          <>
            <fieldset>
              <Description>
                Select the Input type you want to load the message from.
              </Description>
              <StyledSelect id="inputTypeSelect"
                            aria-label="input type select"
                            type="select"
                            value={selectedInputType ?? 'placeholder'}
                            disabled={!!selectedInputId}
                            onChange={(e) => setSelectedInputType(e.target.value)}>
                <option value="placeholder" disabled>Select an Input type</option>
                <option value="server">Server Input</option>
                <option value="forwarder">Forwarder Input</option>
              </StyledSelect>
            </fieldset>

            {selectedInputType === 'server' && (
              <ServerInputSelect selectedInputId={selectedInputId}
                                 inputs={inputs}
                                 onChange={onClick}
                                 isLoading={loading} />
            )}
            {selectedInputType === 'forwarder' && (
              <ForwarderInputSelect selectedInputId={selectedInputId} onChange={onClick} isLoading={loading} />
            )}
          </>
        ) : (
          <ServerInputSelect selectedInputId={selectedInputId} inputs={inputs} onChange={onClick} isLoading={loading} />
        )}
    </LoaderContainer>
  );
};

RecentMessageLoader.propTypes = {
  inputs: PropTypes.object,
  onMessageLoaded: PropTypes.func.isRequired,
  selectedInputId: PropTypes.string,
};

RecentMessageLoader.defaultProps = {
  inputs: undefined,
  selectedInputId: undefined,
};

export default RecentMessageLoader;
