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

import PipelineDetails from './PipelineDetails';

class NewPipeline extends React.Component {
  static propTypes = {
    onChange: PropTypes.func.isRequired,
  };

  _onChange = (newPipeline) => {
    this.props.onChange(newPipeline, this._goToPipeline);
  };

  _goToPipeline = (pipeline) => {
    history.push(Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id));
  };

  _goBack = () => {
    history.goBack();
  };

  render() {
    return (
      <Row>
        <Col md={6}>
          <p>
            Give a name and description to the new pipeline. You can add stages to it when you save the changes.
          </p>
          <PipelineDetails create onChange={this._onChange} onCancel={this._goBack} />
        </Col>
      </Row>
    );
  }
}

export default NewPipeline;
