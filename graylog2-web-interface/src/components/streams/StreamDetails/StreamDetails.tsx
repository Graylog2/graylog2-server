import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { useState } from 'react';

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

import StreamDataRoutingIntake from './StreamDataRoutingIntake';
import StreamDataRoutingProcessing from './StreamDataRoutingProcessing';
import StreamDataRoutingDestinations from './StreamDataRoutingDestinations';
import SectionGrid from 'components/common/Section/SectionGrid';
import { PluginStore } from 'graylog-web-plugin/plugin';

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
        <RightCol>
        </RightCol>
      </Header>

      <Row className="content no-bm">
        <Col xs={12}>
          <StyledSectionGrid $columns='1fr 8fr'>
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
          {currentSegment === DESTINATIONS_SEGMENT && <StreamDataRoutingDestinations stream={stream}/>}
        </FullHeightCol>
      </SegmentContainer>
    </Container>
    </>
  );
};

export default StreamDetails;
