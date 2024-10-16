import React from 'react';

import { Row, Col } from 'components/bootstrap';
import Routes from 'routing/Routes';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';
import useHistory from 'routing/useHistory';

import PipelineDetails from './PipelineDetails';

type Props = {
  onChange: (pipeline: PipelineType, callback?: (pipeline: PipelineType) => void) => void;
};

const NewPipeline = ({ onChange }: Props) => {
  const history = useHistory();

  const _goToPipeline = (pipeline) => {
    history.push(Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id));
  };

  const _goBack = () => {
    history.goBack();
  };

  const _onChange = (newPipeline) => {
    onChange(newPipeline, _goToPipeline);
  };

  return (
    <Row>
      <Col md={6}>
        <p>
          Give a name and description to the new pipeline. You can add stages to it when you save the changes.
        </p>
        <PipelineDetails create onChange={_onChange} onCancel={_goBack} />
      </Col>
    </Row>
  );
};

export default NewPipeline;
