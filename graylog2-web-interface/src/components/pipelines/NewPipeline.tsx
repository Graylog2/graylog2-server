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
import React from 'react';

import { Row, Col } from 'components/graylog';
import history from 'util/History';
import Routes from 'routing/Routes';
import { PipelineType } from 'stores/pipelines/PipelinesStore';

import PipelineDetails from './PipelineDetails';

type Props = {
  onChange: (pipeline: PipelineType, callback?: (pipeline: PipelineType) => void) => void;
};

const NewPipeline = ({ onChange }: Props) => {
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

NewPipeline.propTypes = {
  onChange: PropTypes.func.isRequired,
};

export default NewPipeline;
