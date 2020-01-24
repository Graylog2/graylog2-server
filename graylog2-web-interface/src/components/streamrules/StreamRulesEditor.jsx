import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import styled from 'styled-components';

import Routes from 'routing/Routes';

import { Alert, Button } from 'components/graylog';
import { Icon } from 'components/common';
import LoaderTabs from 'components/messageloaders/LoaderTabs';
import MatchingTypeSwitcher from 'components/streams/MatchingTypeSwitcher';
import StreamRuleList from 'components/streamrules/StreamRuleList';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import Spinner from 'components/common/Spinner';

import StoreProvider from 'injection/StoreProvider';

const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

const StreamAlertHeader = styled.h4`
  font-weight: bold;
  margin: 0 0 12px;
`;

class StreamRulesEditor extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    streamId: PropTypes.string.isRequired,
    messageId: PropTypes.string,
    index: PropTypes.string,
  }

  static defaultProps = {
    messageId: '',
    index: '',
  }

  state = {};

  componentDidMount() {
    this.loadData();
    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  }

  componentWillUnmount() {
    StreamsStore.unregister(this.loadData);
    StreamRulesStore.unregister(this.loadData);
  }

  onMessageLoaded = (message) => {
    this.setState({ message: message });
    if (message !== undefined) {
      const { streamId } = this.props;

      StreamsStore.testMatch(streamId, { message: message.fields }, (resultData) => {
        this.setState({ matchData: resultData });
      });
    } else {
      this.setState({ matchData: undefined });
    }
  };

  loadData = () => {
    const { streamId } = this.props;
    const { message } = this.state;

    StreamRulesStore.types().then((types) => {
      this.setState({ streamRuleTypes: types });
    });

    StreamsStore.get(streamId, (stream) => {
      this.setState({ stream: stream });
    });

    if (message) {
      this.onMessageLoaded(message);
    }
  };

  _onStreamRuleFormSubmit = (streamRuleId, data) => {
    const { streamId } = this.props;
    StreamRulesStore.create(streamId, data, () => {});
  };

  _onAddStreamRule = (event) => {
    event.preventDefault();
    this.newStreamRuleForm.open();
  };

  _getListClassName = (matchData) => {
    return (matchData.matches ? 'success' : 'danger');
  };

  _explainMatchResult = () => {
    const { matchData } = this.state;

    if (matchData) {
      if (matchData.matches) {
        return (
          <>
            <Icon name="check" style={{ color: '#00AE42', marginRight: 3 }} /> This message would be routed to this stream!
          </>
        );
      }
      return (
        <>
          <Icon name="remove" style={{ color: '#AD0707', marginRight: 3 }} /> This message would not be routed to this stream.
        </>
      );
    }

    return ('Please load a message in Step 1 above to check if it would match against these rules.');
  };

  render() {
    const { matchData, stream, streamRuleTypes } = this.state;
    const { currentUser, messageId, index } = this.props;
    const styles = (matchData ? this._getListClassName(matchData) : 'info');

    if (stream && streamRuleTypes) {
      return (
        <div className="row content">
          <div className="col-md-12 streamrule-sample-message">
            <h2>1. Load a message to test rules</h2>

            <div className="stream-loader">
              <LoaderTabs messageId={messageId}
                          index={index}
                          onMessageLoaded={this.onMessageLoaded} />
            </div>

            <div className="spinner" style={{ display: 'none' }}>
              <h2><Icon name="spinner" spin /> &nbsp;Loading message</h2>
            </div>

            <div className="sample-message-display" style={{ display: 'none', marginTop: '5px' }}>
              <strong>Next step:</strong>
              Add/delete/modify stream rules in step 2 and see if the example message would have been
              routed into the stream or not. Use the button on the right to add a stream rule.
            </div>

            <hr />

            <div className="buttons pull-right">
              <Button bsStyle="success"
                      className="show-stream-rule"
                      onClick={this._onAddStreamRule}>
                Add stream rule
              </Button>
              <StreamRuleForm ref={(newStreamRuleForm) => { this.newStreamRuleForm = newStreamRuleForm; }}
                              title="New Stream Rule"
                              streamRuleTypes={streamRuleTypes}
                              onSubmit={this._onStreamRuleFormSubmit} />
            </div>

            <h2>2. Manage stream rules</h2>

            <MatchingTypeSwitcher stream={stream} onChange={this.loadData} />
            <Alert bsStyle={styles}>
              <StreamAlertHeader>{this._explainMatchResult()}</StreamAlertHeader>
              <StreamRuleList stream={stream}
                              streamRuleTypes={streamRuleTypes}
                              permissions={currentUser.permissions}
                              matchData={matchData} />
            </Alert>

            <p style={{ marginTop: '10px' }}>
              <LinkContainer to={Routes.STREAMS}>
                <Button bsStyle="success">I&apos;m done!</Button>
              </LinkContainer>
            </p>
          </div>
        </div>
      );
    }

    return (
      <div className="row content">
        <div style={{ marginLeft: 10 }}>
          <Spinner />
        </div>
      </div>
    );
  }
}

export default StreamRulesEditor;
