import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col } from 'react-bootstrap';

import { Timestamp } from 'components/common';
import PipelineForm from './PipelineForm';

import { MetricContainer, CounterRate } from 'components/metrics';

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

    const pipeline = this.props.pipeline;
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
