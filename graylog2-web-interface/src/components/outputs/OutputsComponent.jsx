import React from 'react';
import { Row, Col } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const OutputsStore = StoreProvider.getStore('Outputs');
const StreamsStore = StoreProvider.getStore('Streams');

import UserNotification from 'util/UserNotification';
import PermissionsMixin from 'util/PermissionsMixin';
import Spinner from 'components/common/Spinner';
import OutputList from './OutputList';
import CreateOutputDropdown from './CreateOutputDropdown';
import AssignOutputDropdown from './AssignOutputDropdown';

const OutputsComponent = React.createClass({
  mixins: [PermissionsMixin],
  componentDidMount() {
    this.loadData();
  },
  loadData() {
    const callback = (resp) => {
      this.setState({
        outputs: resp.outputs,
      });
      if (this.props.streamId) {
        this._fetchAssignableOutputs(resp.outputs);
      }
    };
    if (this.props.streamId) {
      OutputsStore.loadForStreamId(this.props.streamId, callback);
    } else {
      OutputsStore.load(callback);
    }

    OutputsStore.loadAvailableTypes((resp) => {
      this.setState({ types: resp.types });
    });
  },
  getInitialState() {
    return {
    };
  },
  _handleUpdate() {
    this.loadData();
  },
  _handleCreateOutput(data) {
    OutputsStore.save(data, (result) => {
      this.setState({ typeName: 'placeholder' });
      if (this.props.streamId) {
        StreamsStore.addOutput(this.props.streamId, result.id, (response) => {
          this._handleUpdate();
          return response;
        });
      } else {
        this._handleUpdate();
      }
      return result;
    });
  },
  _fetchAssignableOutputs(outputs) {
    OutputsStore.load((resp) => {
      const streamOutputIds = outputs.map((output) => { return output.id; });
      const assignableOutputs = resp.outputs
        .filter((output) => { return streamOutputIds.indexOf(output.id) === -1; })
        .sort((output1, output2) => { return output1.title.localeCompare(output2.title); });
      this.setState({ assignableOutputs: assignableOutputs });
    });
  },
  _handleAssignOutput(outputId) {
    StreamsStore.addOutput(this.props.streamId, outputId, (response) => {
      this._handleUpdate();
      return response;
    });
  },
  _removeOutputGlobally(outputId) {
    if (window.confirm('Do you really want to terminate this output?')) {
      OutputsStore.remove(outputId, (response) => {
        UserNotification.success('Output was terminated.', 'Success');
        this._handleUpdate();
        return response;
      });
    }
  },
  _removeOutputFromStream(outputId, streamId) {
    if (window.confirm('Do you really want to remove this output from the stream?')) {
      StreamsStore.removeOutput(streamId, outputId, (response) => {
        UserNotification.success('Output was removed from stream.', 'Success');
        this._handleUpdate();
        return response;
      });
    }
  },
  _handleOutputUpdate(output, deltas) {
    OutputsStore.update(output, deltas, () => {
      this._handleUpdate();
    });
  },
  render() {
    if (this.state.outputs && this.state.types && (!this.props.streamId || this.state.assignableOutputs)) {
      const permissions = this.props.permissions;
      const streamId = this.props.streamId;
      const createOutputDropdown = (this.isPermitted(permissions, ['outputs:create']) ?
        (<CreateOutputDropdown types={this.state.types} onSubmit={this._handleCreateOutput}
                              getTypeDefinition={OutputsStore.loadAvailable} streamId={streamId} />) : null);
      const assignOutputDropdown = (streamId ?
        (<AssignOutputDropdown ref="assignOutputDropdown" streamId={streamId}
                              outputs={this.state.assignableOutputs}
                              onSubmit={this._handleAssignOutput} />) : null);
      return (
        <div className="outputs">
          <Row className="content">
            <Col md={4}>
              {createOutputDropdown}
            </Col>
            <Col md={8}>
              {assignOutputDropdown}
            </Col>
          </Row>

          <OutputList ref="outputList" streamId={streamId} outputs={this.state.outputs} permissions={permissions}
                      getTypeDefinition={OutputsStore.loadAvailable} types={this.state.types}
                      onRemove={this._removeOutputFromStream} onTerminate={this._removeOutputGlobally}
                      onUpdate={this._handleOutputUpdate} />
        </div>
      );
    }
    return <Spinner />;
  },
});
export default OutputsComponent;
