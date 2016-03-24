import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Input, Alert, Button } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const UsersStore = StoreProvider.getStore('Users');
const StreamsStore = StoreProvider.getStore('Streams');

import {TypeAheadInput} from 'components/common';
import AlertReceiver from 'components/alerts/AlertReceiver';

const AlertReceiversList = React.createClass({
  propTypes: {
    receivers: React.PropTypes.object,
    streamId: React.PropTypes.string.isRequired,
  },
  mixins: [PermissionsMixin, Reflux.connect(CurrentUserStore)],
  getDefaultProps() {
    return {
      receivers: {users: [], emails: []},
    };
  },
  getInitialState() {
    return {
      usernames: [],
      userReceiver: '',
      emailReceiver: '',
    };
  },
  componentDidMount() {
    UsersStore.loadUsers().then((users) => this.setState({usernames: users.map(user => user.username)}));
  },
  _getEffectiveReceivers(receivers) {
    const effectiveReceivers = {};

    effectiveReceivers.users = receivers.users ? receivers.users : [];
    effectiveReceivers.emails = receivers.emails ? receivers.emails : [];

    return effectiveReceivers;
  },
  _onChangeUser(evt) {
    this.setState({userReceiver: evt.target.value});
  },
  _onChangeEmail(evt) {
    this.setState({emailReceiver: evt.target.value});
  },
  _addUserReceiver(evt) {
    evt.preventDefault();
    StreamsStore.addReceiver(this.props.streamId, 'users', this.refs.user.getValue(), () => {
      this.refs.user.clear();
      this.setState({userReceiver: ''});
    });
  },
  _addEmailReceiver(evt) {
    evt.preventDefault();
    StreamsStore.addReceiver(this.props.streamId, 'emails', this.refs.email.getValue(), () => {
      this.setState({emailReceiver: ''});
    });
  },
  _formatReceiverList(receivers) {
    if (!receivers || (receivers.users.length === 0 && receivers.emails.length === 0)) {
      return <Alert bsStyle="info">No configured alert receivers.</Alert>;
    }

    const userReceivers = receivers.users.map((receiver) => {
      return <AlertReceiver key={'users-' + receiver} type="users" receiver={receiver} streamId={this.props.streamId}/>;
    });
    const emailReceivers = receivers.emails.map((receiver) => {
      return <AlertReceiver key={'email-' + receiver} type="emails" receiver={receiver} streamId={this.props.streamId}/>;
    });
    return (
      <ul className="alert-receivers">
        {userReceivers}
        {emailReceivers}
      </ul>
    );
  },
  _getSuggestions() {
    const effectiveReceivers = this._getEffectiveReceivers(this.props.receivers);
    return this.state.usernames.filter(user => effectiveReceivers.users.indexOf(user) === -1);
  },
  render() {
    return (
      <span>
        {this._formatReceiverList(this._getEffectiveReceivers(this.props.receivers))}
        {this.isPermitted(this.state.currentUser.permissions, 'streams:edit:' + this.props.streamId) &&
          <Row id="add-alert-receivers" className="row-sm">

            <Col md={6}>
              <form className="form-inline" onSubmit={this._addUserReceiver} >
                <TypeAheadInput ref="user"
                                suggestions={this._getSuggestions()}
                                label="Username:"
                                displayKey="value"
                                autoComplete="off"
                                required/>
                {' '}
                <Button type="submit" bsStyle="success">Subscribe</Button>
              </form>
            </Col>

            <Col md={6}>
              <form className="form-inline" onSubmit={this._addEmailReceiver} >
                <Input ref="email" label="Email address:" type="text" value={this.state.emailReceiver} onChange={this._onChangeEmail}/>
                {' '}
                <Button type="submit" bsStyle="success">Subscribe</Button>
              </form>
            </Col>

          </Row>
        }
      </span>
    );
  },
});

export default AlertReceiversList;
