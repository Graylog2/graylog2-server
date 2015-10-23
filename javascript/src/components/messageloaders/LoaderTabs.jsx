import React, {PropTypes} from 'react';
import { Tab, Tabs, Col } from 'react-bootstrap';
import Immutable from 'immutable';

import InputsStore from 'stores/inputs/InputsStore';

import RecentMessageLoader from './RecentMessageLoader';
import MessageShow from 'components/search/MessageShow';
import MessageLoader from 'components/extractors/MessageLoader';

const LoaderTabs = React.createClass({
  propTypes: {
    messageId: PropTypes.string,
    index: PropTypes.string,
    onMessageLoaded: PropTypes.func,
    selectedInputId: PropTypes.string,
    customFieldActions: PropTypes.node,
  },
  getInitialState() {
    return {
      message: undefined,
      inputs: undefined,
    };
  },
  componentDidMount() {
    this.loadData();
    if (this.props.messageId && this.props.index) {
      this.refs.messageLoader.submit(this.props.messageId, this.props.index);
    }
  },
  onMessageLoaded(message) {
    if (!message.formatted_fields) {
      message.formatted_fields = {};
    }
    message.formatted_fields.timestamp = message.timestamp;
    message.fields._id = message.id;
    this.setState({message: message});
    if (this.props.onMessageLoaded) {
      this.props.onMessageLoaded(message);
    }
  },
  loadData() {
    InputsStore.list((inputsList) => {
      const inputs = {};
      inputsList.forEach(input => {
        inputs[input.input_id] = input;
      });
      this.setState({inputs: Immutable.Map(inputs)});
    });
  },
  render() {
    let displayMessage;
    if (this.state.message && this.state.inputs) {
      displayMessage = (
        <Col md={12}>
          <MessageShow message={this.state.message} inputs={this.state.inputs}
                       disableTestAgainstStream
                       disableFieldActions={!this.props.customFieldActions}
                       customFieldActions={this.props.customFieldActions}/>
        </Col>
      );
    }

    let defaultActiveKey;
    if (this.props.messageId && this.props.index) {
      defaultActiveKey = 2;
    } else {
      defaultActiveKey = 1;
    }
    return (
      <div>
        <Tabs defaultActiveKey={defaultActiveKey} animation={false}>
          <Tab eventKey={1} title="Recent" style={{marginBottom: 10}}>
            <RecentMessageLoader inputs={this.state.inputs}
                                 selectedInputId={this.props.selectedInputId}
                                 onMessageLoaded={this.onMessageLoaded}/>
          </Tab>
          <Tab eventKey={2} title="Manual" style={{marginBottom: 10}}>
            <div style={{marginTop: 5, marginBottom: 15}}>
              Please provide the id and index of the message that you want to load in this form:
            </div>

            <MessageLoader ref="messageLoader" onMessageLoaded={this.onMessageLoaded} hidden={false} hideText/>
          </Tab>
        </Tabs>
        {displayMessage}
      </div>
    );
  },
});

export default LoaderTabs;
