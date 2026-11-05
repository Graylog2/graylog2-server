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
import { useTranslation } from 'react-i18next';
import type * as Immutable from 'immutable';
import styled from 'styled-components';

import { Row, Col, Input } from 'components/bootstrap';
import AppConfig from 'util/AppConfig';
import InputDropdown from 'components/inputs/InputDropdown';
import UserNotification from 'util/UserNotification';
import type { Message } from 'views/components/messagelist/Types';
import useForwarderMessageLoaders from 'components/messageloaders/useForwarderMessageLoaders';
import universalSearch from 'stores/search/UniversalSearch';

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
  inputs: Immutable.Map<string, InputType>;
  onChange: (inputId: string) => void;
  selectedInputId: string | undefined;
  isLoading: boolean;
};

const ServerInputSelect = ({ selectedInputId, inputs, onChange, isLoading }: ServerInputSelectProps) => {
  const { t } = useTranslation('messageloaders');

  return (
    <fieldset>
      <Description>
        {selectedInputId ? t('recent.server_description_selected') : t('recent.server_description')}
      </Description>
      <InputDropdown
        inputs={inputs}
        preselectedInputId={selectedInputId}
        onLoadMessage={onChange}
        title={isLoading ? t('loading_message_button') : t('recent.load_message')}
        disabled={isLoading}
      />
    </fieldset>
  );
};

type ForwaderInputSelectProps = {
  onChange: (inputId: string) => void;
  selectedInputId: string | undefined;
  isLoading: boolean;
};

const ForwarderInputSelect = ({ selectedInputId, onChange, isLoading }: ForwaderInputSelectProps) => {
  const { t } = useTranslation('messageloaders');
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  return (
    <fieldset>
      <Description>{t('recent.forwarder_description')}</Description>
      <Row>
        <Col md={8}>
          <ForwarderInputDropdown
            onLoadMessage={onChange}
            title={isLoading ? t('loading_message_button') : t('recent.load_message')}
            preselectedInputId={selectedInputId}
            loadButtonDisabled={isLoading}
          />
        </Col>
      </Row>
    </fieldset>
  );
};

type Props = {
  inputs?: Immutable.Map<string, InputType>;
  onMessageLoaded: (message?: Message) => void;
  selectedInputId?: string;
};

const RecentMessageLoader = ({ inputs = undefined, onMessageLoaded, selectedInputId = undefined }: Props) => {
  const { t } = useTranslation('messageloaders');
  const [loading, setLoading] = useState<boolean>(false);
  const { ForwarderInputDropdown } = useForwarderMessageLoaders();

  const [selectedInputType, setSelectedInputType] = useState<'forwarder' | 'server'>(
    ForwarderInputDropdown ? undefined : 'server',
  );
  const isCloud = AppConfig.isCloud();

  useEffect(() => {
    if (selectedInputId && inputs) {
      setSelectedInputType(inputs?.get(selectedInputId) ? 'server' : 'forwarder');
    }
  }, [inputs, selectedInputId]);

  const onClick = async (inputId: string) => {
    const input = inputs && inputs.get(inputId);

    if (selectedInputType === 'server' && !input) {
      UserNotification.error(
        t('recent.invalid_input_selected', { inputId }),
        t('recent.invalid_input_title', { inputId }),
      );

      return;
    }

    try {
      setLoading(true);
      const response = await universalSearch(
        'relative',
        `gl2_source_input:${inputId} OR gl2_source_radio_input:${inputId}`,
        { type: 'relative', range: 3600 },
        undefined,
        1,
        undefined,
        undefined,
        undefined,
        false,
      );

      if (response.total_results > 0) {
        onMessageLoaded(response.messages[0]);
      } else {
        UserNotification.error(t('recent.no_recent_message'));
        onMessageLoaded(undefined);
      }
    } finally {
      setLoading(false);
    }
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
      {ForwarderInputDropdown ? (
        <>
          <fieldset>
            <Description>{t('select_input_type_help')}</Description>
            <StyledSelect
              id="inputTypeSelect"
              aria-label={t('recent.input_type_select_label')}
              type="select"
              value={selectedInputType ?? 'placeholder'}
              disabled={!!selectedInputId}
              onChange={(e) => setSelectedInputType(e.target.value as 'forwarder' | 'server')}>
              <option value="placeholder" disabled>
                {t('select_input_type_placeholder')}
              </option>
              <option value="server">{t('server_input')}</option>
              <option value="forwarder">{t('forwarder_input')}</option>
            </StyledSelect>
          </fieldset>

          {selectedInputType === 'server' && (
            <ServerInputSelect
              selectedInputId={selectedInputId}
              inputs={inputs}
              onChange={onClick}
              isLoading={loading}
            />
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

export default RecentMessageLoader;
