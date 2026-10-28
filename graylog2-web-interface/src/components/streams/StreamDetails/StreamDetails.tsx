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
import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';
import URI from 'urijs';
import upperCase from 'lodash/upperCase';

import Routes from 'routing/Routes';
import { Button, Col, MenuItem, Row, SegmentedControl } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import { Icon, IfPermitted } from 'components/common';
import type { Stream } from 'logic/streams/types';
import useStreamMutations from 'hooks/useStreamMutations';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useQuery from 'routing/useQuery';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { MoreActionsMenu } from 'components/common/MoreActions';

import StreamDataRoutingIntake from './StreamDataRoutingIntake';
import StreamDataRoutingProcessing from './StreamDataRoutingProcessing';
import StreamDataRoutingDestinations from './StreamDataRoutingDestinations';

import StreamModal from '../StreamModal';
import ThroughputCell from '../StreamsOverview/cells/ThroughputCell';

type Props = {
  stream: Stream;
};

const INTAKE_SEGMENT = 'intake';
const PROCESSING_SEGMENT = 'processing';
const DESTINATIONS_SEGMENT = 'destinations';

type DetailsSegment = 'intake' | 'processing' | 'destinations';

const Container = styled.div`
  display: flex;
  height: 100%;
  flex-direction: column;
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  padding-top: 30px;
  margin-bottom: 20px;
  gap: 15px;
  margin-left: -15px;
  margin-right: -15px;
`;

const LeftCol = styled.div`
  display: flex;
  gap: 20px;
  align-items: center;
`;

const RightCol = styled.div`
  display: flex;
  gap: 30px;
`;

const MainDetailsRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
`;

const SegmentContainer = styled(Row)`
  flex: 1;
`;

const FullHeightCol = styled(Col)`
  height: 100%;
`;

const StyledSectionGrid = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    align-content: center;
    gap: ${theme.spacings.md};
  `,
);
const StyledSegmentedControl = styled(SegmentedControl)(
  ({ theme }) => css`
    background-color: ${theme.colors.section.filled.background};
    border: 1px solid ${theme.colors.section.filled.border};

    .mantine-SegmentedControl-innerLabel {
      vertical-align: middle;
    }

    .mantine-SegmentedControl-indicator {
      height: 70% !important;
    }
  `,
);
const ThroughputCol = styled(Col)`
  display: flex;
  align-items: center;
  height: 100%;
  flex-flow: column wrap;
  place-content: flex-end space-evenly;
`;

const StreamDetails = ({ stream }: Props) => {
  const { t } = useTranslation('streams');
  const segmentsDetails = [
    { value: 'intake' as const, label: t('stream_details.segment_intake') },
    { value: 'processing' as const, label: t('stream_details.segment_processing') },
    { value: 'destinations' as const, label: t('stream_details.segment_destinations') },
  ];
  const pageDescriptionKeys = {
    [INTAKE_SEGMENT]: 'stream_details.intake_description',
    [PROCESSING_SEGMENT]: 'stream_details.processing_description',
    [DESTINATIONS_SEGMENT]: 'stream_details.destination_description',
  };
  const { segment } = useQuery();
  const [currentSegment, setCurrentSegment] = useState<DetailsSegment>((segment as DetailsSegment) || INTAKE_SEGMENT);
  const DataLakeJobComponent = PluginStore.exports('dataLake')?.[0]?.DataLakeJobs;
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const {
    data: { indexSets },
  } = useIndexSetsList(false);
  const { updateStream } = useStreamMutations();
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();

  const updateURLStepQueryParam = (nextSegment: DetailsSegment) => {
    const newUrl = new URI(window.location.href).removeSearch('segment').addQuery('segment', nextSegment);
    history.replace(newUrl.resource());
  };

  const onSegmentChange = (nextSegment: DetailsSegment) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS[`STREAM_ITEM_DATA_ROUTING_${upperCase(nextSegment)}_OPENED`], {
      app_pathname: 'streams',
    });

    setCurrentSegment(nextSegment);
    updateURLStepQueryParam(nextSegment);
  };

  const toggleUpdateModal = useCallback(() => {
    setShowUpdateModal((cur) => !cur);

    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_UPDATE_CLICKED, {
      app_pathname: 'streams',
    });
  }, [sendTelemetry]);
  const onUpdate = useCallback(
    (newStream: Stream) =>
      updateStream({ streamId: stream.id, data: newStream }).then(() => {
        UserNotification.success(
          t('stream_details.updated', { title: newStream.title }),
          t('stream_details.success_title'),
        );
      }),
    [stream.id, updateStream, t],
  );

  return (
    <>
      {DataLakeJobComponent && <DataLakeJobComponent streamId={stream.id} />}
      <Container>
        <Header>
          <LeftCol>
            <Button onClick={() => history.push(Routes.STREAMS)}>
              <Icon name="arrow_left_alt" size="sm" /> {t('stream_details.back')}
            </Button>

            <h1>{t('stream_details.heading', { title: stream.title })}</h1>

            <IfPermitted permissions={`streams:edit:${stream.id}`}>
              <MoreActionsMenu id="stream-actions">
                <MenuItem onClick={() => toggleUpdateModal()}>{t('stream_details.edit')}</MenuItem>
              </MoreActionsMenu>
            </IfPermitted>
          </LeftCol>
          <RightCol />
        </Header>

        <Row className="content no-bm">
          <Col xs={10}>
            <StyledSectionGrid>
              <h3>{t('stream_details.data_routing')}</h3>
              <MainDetailsRow>
                <StyledSegmentedControl<DetailsSegment>
                  data={segmentsDetails}
                  radius="sm"
                  value={currentSegment}
                  onChange={onSegmentChange}
                />
              </MainDetailsRow>
            </StyledSectionGrid>
            <p className="description">
              <span>{t(pageDescriptionKeys[currentSegment])}</span>
            </p>
          </Col>
          <ThroughputCol xs={2}>
            <strong>{t('stream_details.throughput')}</strong>
            <ThroughputCell stream={stream} />
          </ThroughputCol>
        </Row>
        <SegmentContainer className="content">
          <FullHeightCol xs={12}>
            {currentSegment === INTAKE_SEGMENT && <StreamDataRoutingIntake stream={stream} />}
            {currentSegment === PROCESSING_SEGMENT && <StreamDataRoutingProcessing stream={stream} />}
            {currentSegment === DESTINATIONS_SEGMENT && <StreamDataRoutingDestinations stream={stream} />}
          </FullHeightCol>
        </SegmentContainer>
        {showUpdateModal && (
          <StreamModal
            title={t('stream_details.edit_modal_title')}
            onSubmit={onUpdate}
            onClose={toggleUpdateModal}
            submitButtonText={t('stream_details.update_button')}
            submitLoadingText={t('stream_details.updating')}
            initialValues={stream}
            indexSets={indexSets}
          />
        )}
      </Container>
    </>
  );
};

export default StreamDetails;
