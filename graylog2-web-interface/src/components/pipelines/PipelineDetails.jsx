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
import { Timestamp } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';

import PipelineForm from './PipelineForm';

class PipelineDetails extends React.Component {
  static propTypes = {
    pipeline: PropTypes.object,
    create: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func,
  };

  render() {
    if (this.props.create) {
      return <PipelineForm create save={this.props.onChange} onCancel={this.props.onCancel} modal={false} />;
    }

    const { pipeline } = this.props;

    return (
      <div>
        <Row>
          <Col md={12}>
            <div className="pull-right">
              <PipelineForm pipeline={pipeline} save={this.props.onChange} />
            </div>
            <h2>Details</h2>
            <dl className="dl-horizontal pipeline-dl" style={{ marginTop: 10 }}>
              <dt>Title</dt>
              <dd>{pipeline.title}</dd>
              <dt>Description</dt>
              <dd>{pipeline.description}</dd>
              <dt>Created</dt>
              <dd><Timestamp dateTime={pipeline.created_at} relative /></dd>
              <dt>Last modified</dt>
              <dd><Timestamp dateTime={pipeline.modified_at} relative /></dd>
              <dt>Current throughput</dt>
              <dd>
                <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.executed`}>
                  <CounterRate suffix="msg/s" />
                </MetricContainer>
              </dd>
            </dl>
          </Col>
        </Row>
        <hr />
      </div>
    );
  }
}

export default PipelineDetails;
