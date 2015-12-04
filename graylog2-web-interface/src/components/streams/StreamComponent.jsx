import React from 'react';
import { Row, Col } from 'react-bootstrap';

import StreamsStore from 'stores/streams/StreamsStore';
import StreamRulesStore from 'stores/streams/StreamRulesStore';
import UsersStore from 'stores/users/UsersStore';

import PermissionsMixin from 'util/PermissionsMixin';
import UserNotification from 'util/UserNotification';
import DocsHelper from 'util/DocsHelper';

import StreamList from './StreamList';
import CreateStreamButton from './CreateStreamButton';
import Spinner from 'components/common/Spinner';
import DocumentationLink from 'components/support/DocumentationLink';
import PageHeader from 'components/common/PageHeader';

const StreamComponent = React.createClass({
  propTypes: {
    username: React.PropTypes.string.isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
  },
  mixins: [PermissionsMixin],
  getInitialState() {
    return {};
  },
  componentDidMount() {
    this.loadData();
    StreamRulesStore.types().then((types) => {
      this.setState({streamRuleTypes: types});
    });
    UsersStore.load(this.props.username).then((user) => {
      this.setState({user: user});
    });
    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  },
  loadData() {
    StreamsStore.load((streams) => {
      this.setState({streams: streams});
    });
  },
  _onSave(streamId, stream) {
    StreamsStore.save(stream, () => {
      UserNotification.success('Stream has been successfully created.', 'Success');
    });
  },
  render() {
    const createStreamButton = (this.isPermitted(this.props.permissions, ['streams:create']) ?
      <CreateStreamButton ref="createStreamButton" bsSize="large" bsStyle="success" onSave={this._onSave} /> :
      null);

    const pageHeader = (
      <PageHeader title="Streams">
        <span>You can route incoming messages into streams by applying rules against them. If a
          message
          matches all rules of a stream it is routed into it. A message can be routed into
          multiple
          streams. You can for example create a stream that contains all SSH logins and configure
          to be alerted whenever there are more logins than usual.

          Read more about streams in the <DocumentationLink page={DocsHelper.PAGES.STREAMS} text="documentation"/>.</span>

        <span>
          Take a look at the
          {' '}<DocumentationLink page={DocsHelper.PAGES.EXTERNAL_DASHBOARDS} text="Graylog stream dashboards"/>{' '}
          for wall-mounted displays or other integrations.
        </span>

        {createStreamButton}
      </PageHeader>
    );

    if (this.state.streams && this.state.streamRuleTypes) {
      return (
        <div>
          {pageHeader}

          <Row className="content">
            <Col md={12}>
              <StreamList streams={this.state.streams} streamRuleTypes={this.state.streamRuleTypes}
                          permissions={this.props.permissions} user={this.state.user}
                          onStreamCreated={this._onSave}/>
            </Col>
          </Row>
        </div>
      );
    }

    return (
      <div>
        {pageHeader}

        <Row className="content">
          <div style={{marginLeft: 10}}>
            <Spinner/>
          </div>
        </Row>
      </div>
    );
  },
});

module.exports = StreamComponent;
