import React from 'react';
import styled from 'styled-components';

import { Row, Col } from 'components/bootstrap';
import { RelativeTime } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';

import PipelineForm from './PipelineForm';

const PipelineDl = styled.dl`
  & {
    margin-bottom: 0;
    margin-top: 10px;
  }

  & > dt {
    text-align: left;
    width: 160px;
  }

  & > dt::after {
    content: ':';
  }

  & > dd {
    margin-left: 120px;
  }
`;

type Props = {
  pipeline?: PipelineType,
  create?: boolean,
  onChange: (event) => void,
  onCancel?: () => void,
};

const PipelineDetails = ({ pipeline, create = false, onChange, onCancel = () => {} }: Props) => {
  if (create) {
    return <PipelineForm create save={onChange} onCancel={onCancel} modal={false} />;
  }

  return (
    <div>
      <Row>
        <Col md={12}>
          <div className="pull-right">
            <PipelineForm pipeline={pipeline} save={onChange} />
          </div>
          <h2>Details</h2>
          <PipelineDl className="dl-horizontal">
            <dt>Title</dt>
            <dd>{pipeline.title}</dd>
            <dt>Description</dt>
            <dd>{pipeline.description}</dd>
            <dt>Created</dt>
            <dd><RelativeTime dateTime={pipeline.created_at} /></dd>
            <dt>Last modified</dt>
            <dd><RelativeTime dateTime={pipeline.modified_at} /></dd>
            <dt>Current throughput</dt>
            <dd>
              <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.executed`}>
                <CounterRate suffix="msg/s" />
              </MetricContainer>
            </dd>
          </PipelineDl>
        </Col>
      </Row>
      <hr />
    </div>
  );
};

export default PipelineDetails;
