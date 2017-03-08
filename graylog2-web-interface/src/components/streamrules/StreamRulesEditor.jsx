import React, { PropTypes } from 'react';
import { Alert, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

import LoaderTabs from 'components/messageloaders/LoaderTabs';
import MatchingTypeSwitcher from 'components/streams/MatchingTypeSwitcher';
import StreamRuleList from 'components/streamrules/StreamRuleList';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import Spinner from 'components/common/Spinner';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

const StreamRulesEditor = React.createClass({
  propTypes() {
    return {
      currentUser: PropTypes.object.isRequired,
      streamId: PropTypes.string.isRequired,
      messageId: PropTypes.string,
      index: PropTypes.string,
    };
  },
  getInitialState() {
    return {};
  },
  componentDidMount() {
    this.loadData();
    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  },
  componentWillUnmount() {
    StreamsStore.unregister(this.loadData);
    StreamRulesStore.unregister(this.loadData);
  },
  onMessageLoaded(message) {
    this.setState({ message: message });
    if (message !== undefined) {
      StreamsStore.testMatch(this.props.streamId, { message: message.fields }, (resultData) => {
        this.setState({ matchData: resultData });
      });
    } else {
      this.setState({ matchData: undefined });
    }
  },
  loadData() {
    StreamRulesStore.types().then((types) => {
      this.setState({ streamRuleTypes: types });
    });

    StreamsStore.get(this.props.streamId, (stream) => {
      this.setState({ stream: stream });
    });

    if (this.state.message) {
      this.onMessageLoaded(this.state.message);
    }
  },
  _onStreamRuleFormSubmit(streamRuleId, data) {
    StreamRulesStore.create(this.props.streamId, data, () => {});
  },
  _onAddStreamRule(event) {
    event.preventDefault();
    this.refs.newStreamRuleForm.open();
  },
  _getListClassName(matchData) {
    return (matchData.matches ? 'success' : 'danger');
  },
  _explainMatchResult() {
    if (this.state.matchData) {
      if (this.state.matchData.matches) {
        return (
          <span>
            <i className="fa fa-check" style={{ color: 'green' }} /> This message would be routed to this stream.
          </span>);
      }
      return (
        <span>
          <i className="fa fa-remove" style={{ color: 'red' }} /> This message would not be routed to this stream.
          </span>);
    }
    return ('Please load a message to check if it would match against these rules and therefore be routed into this stream.');
  },
  render() {
    const styles = (this.state.matchData ? this._getListClassName(this.state.matchData) : 'info');
    if (this.state.stream && this.state.streamRuleTypes) {
      return (
        <div className="row content">
          <div className="col-md-12 streamrule-sample-message">
            <h2>
              1. Load a message to test rules
            </h2>

            <div className="stream-loader">
              <LoaderTabs messageId={this.props.messageId} index={this.props.index} onMessageLoaded={this.onMessageLoaded} />
            </div>

            <div className="spinner" style={{ display: 'none' }}><h2><i
              className="fa fa-spinner fa-spin" /> &nbsp;Loading message</h2></div>

            <div className="sample-message-display" style={{ display: 'none', marginTop: '5px' }}>
              <strong>Next step:</strong>
              Add/delete/modify stream rules in step 2 and see if the example message would have been
              routed into the stream or not. Use the button on the right to add a stream rule.
            </div>

            <hr />

            <div className="buttons pull-right">
              <button className="btn btn-success show-stream-rule" onClick={this._onAddStreamRule}>
                Add stream rule
              </button>
              <StreamRuleForm ref="newStreamRuleForm" title="New Stream Rule"
                              streamRuleTypes={this.state.streamRuleTypes} onSubmit={this._onStreamRuleFormSubmit} />
            </div>

            <h2>
              2. Manage stream rules
            </h2>

            {this._explainMatchResult()}

            <MatchingTypeSwitcher stream={this.state.stream} onChange={this.loadData} />
            <Alert ref="well" bsStyle={styles}>
              <StreamRuleList stream={this.state.stream} streamRuleTypes={this.state.streamRuleTypes}
                              permissions={this.props.currentUser.permissions} matchData={this.state.matchData} />
            </Alert>

            <p style={{ marginTop: '10px' }}>
              <LinkContainer to={Routes.STREAMS}>
                <Button bsStyle="success">I'm done!</Button>
              </LinkContainer>
            </p>
          </div>
        </div>
      );
    }
    return (<div className="row content"><div style={{ marginLeft: 10 }}><Spinner /></div></div>);
  },
});

export default StreamRulesEditor;
