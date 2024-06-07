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
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import {
  Button,
  Col,
  DropdownButton,
  MenuItem,
  Row,
  SegmentedControl,
} from 'components/bootstrap';
import { Icon, IfPermitted } from 'components/common';
import type { Stream } from 'stores/streams/StreamsStore';
import SectionGrid from 'components/common/Section/SectionGrid';

import StreamDataRoutingIntake from './StreamDataRoutingIntake';
import StreamDataRoutingProcessing from './StreamDataRoutingProcessing';
import StreamDataRoutingDestinations from './StreamDataRoutingDestinations';

type Props = {
  stream: Stream,
};
const INTAKE_SEGMENT = 'intake';
const PROCESSING_SEGMENT = 'processing';
const DESTINATIONS_SEGMENT = 'destinations';

const SEGMENTS_DETAILS = [
  {
    value: 'intake' as const,
    label: 'Intake',
  },
  {
    value: 'processing' as const,
    label: 'Processing',
  },
  {
    value: 'destinations' as const,
    label: 'Destinations',
  },
];

type DetailsSegments = 'intake' | 'processing' | 'destinations';

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

const StyledSectionGrid = styled(SectionGrid)`
  align-items: center;
`;

const StreamDetails = ({ stream }: Props) => {
  const navigate = useNavigate();
  const [currentSegment, setCurrentSegment] = useState<DetailsSegments>(INTAKE_SEGMENT);
  const DataWarehouseJobComponent = PluginStore.exports('dataWarehouse')?.[0]?.DataWarehouseJobs;

  return (
    <>
      <DataWarehouseJobComponent />
      <Container>
        <Header>
          <LeftCol>
            <Button onClick={() => navigate(Routes.STREAMS)}>
              <Icon name="arrow_left_alt" size="sm" /> Back
            </Button>

            <h1>{stream.title}</h1>

            <IfPermitted permissions="stream:edit">
              <DropdownButton title={<Icon name="more_horiz" />} id="stream-actions" noCaret bsSize="xs">
                <MenuItem onClick={() => {}}>Edit</MenuItem>
              </DropdownButton>
            </IfPermitted>
          </LeftCol>
          <RightCol />
        </Header>

        <Row className="content no-bm">
          <Col xs={12}>
            <StyledSectionGrid $columns="1fr 8fr">
              <h3>Data Routing</h3>
              <MainDetailsRow>
                <SegmentedControl<DetailsSegments> data={SEGMENTS_DETAILS}
                                                   value={currentSegment}
                                                   onChange={setCurrentSegment} />
              </MainDetailsRow>
            </StyledSectionGrid>
          </Col>
        </Row>
        <SegmentContainer className="content">
          <FullHeightCol xs={12}>
            {currentSegment === INTAKE_SEGMENT && <StreamDataRoutingIntake stream={stream} />}
            {currentSegment === PROCESSING_SEGMENT && <StreamDataRoutingProcessing />}
            {currentSegment === DESTINATIONS_SEGMENT && <StreamDataRoutingDestinations />}
          </FullHeightCol>
        </SegmentContainer>
      </Container>
    </>
  );
};

export default StreamDetails;
