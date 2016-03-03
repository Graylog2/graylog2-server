import React from 'react';
import { Col } from 'react-bootstrap';

import { DataTable, EntityListItem } from 'components/common';
import ConnectionForm from './ConnectionForm';

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
      <tr>
        <td style={{width: 400}}>{pipeline.title}</td>
        <td>{pipeline.description}</td>
      </tr>
    );
  },

  _formatPipelines(pipelines) {
    const headers = ['Title', 'Description'];

    return (
      <DataTable id={`${this.props.stream.id}-pipelines`}
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._pipelineHeaderFormatter}
                 rows={pipelines}
                 dataRowFormatter={this._pipelineRowFormatter}
                 filterLabel=""
                 filterKeys={[]}/>
    );
  },

  render() {
    const actions = (
      <ConnectionForm connection={{stream: this.props.stream, pipelines: this.props.pipelines}}
                      save={this.props.onUpdate}/>
    );

    const content = (
      <Col md={12}>
        {this._formatPipelines(this.props.pipelines)}
      </Col>
    );

    return (
      <EntityListItem title={`Stream "${this.props.stream.title}"`}
                      actions={actions}
                      contentRow={content}/>
    );
  },
});

export default Connection;
