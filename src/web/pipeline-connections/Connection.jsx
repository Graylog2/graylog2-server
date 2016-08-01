import React from 'react';
import { Button, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DataTable, EntityListItem, Timestamp } from 'components/common';
import ConnectionForm from './ConnectionForm';

import Routes from 'routing/Routes';

const Connection = React.createClass({
  propTypes: {
    stream: React.PropTypes.object.isRequired,
    pipelines: React.PropTypes.array.isRequired,
    onUpdate: React.PropTypes.func.isRequired,
  },

  _pipelineHeaderFormatter(header) {
    return <th>{header}</th>;
  },

  _pipelineRowFormatter(pipeline) {
    return (
      <tr key={pipeline.id}>
        <td style={{ width: 400 }}>
          <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_PIPELINEID')(pipeline.id)}><a>{pipeline.title}</a></LinkContainer>
        </td>
        <td>{pipeline.description}</td>
        <td><Timestamp dateTime={pipeline.created_at} relative /></td>
        <td><Timestamp dateTime={pipeline.modified_at} relative /></td>
      </tr>
    );
  },

  _formatPipelines(pipelines) {
    const headers = ['Title', 'Description', 'Created', 'Last modified'];

    return (
      <DataTable id={`${this.props.stream.id}-pipelines`}
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._pipelineHeaderFormatter}
                 rows={pipelines}
                 dataRowFormatter={this._pipelineRowFormatter}
                 noDataText={'This stream has no connected pipelines. Click on "Edit connections" to add one.'}
                 filterLabel=""
                 filterKeys={[]} />
    );
  },

  render() {
    const actions = [
      <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_SIMULATE_STREAMID')(this.props.stream.id)}>
        <Button bsStyle="info" key={`simulate-${this.props.stream.id}`}>Simulate processing</Button>
      </LinkContainer>,
      <ConnectionForm key={`connection-${this.props.stream.id}`}
                      connection={{ stream: this.props.stream, pipelines: this.props.pipelines }}
                      save={this.props.onUpdate} />,
    ];

    const content = (
      <Col md={12}>
        {this._formatPipelines(this.props.pipelines)}
      </Col>
    );

    return (
      <EntityListItem title={`${this.props.stream.title} stream`}
                      titleSuffix={`Connected to ${this.props.pipelines.length === 1 ? '1 pipeline' : `${this.props.pipelines.length} pipelines`}`}
                      description={this.props.stream.description}
                      actions={actions}
                      contentRow={content} />
    );
  },
});

export default Connection;
