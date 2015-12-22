import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Input, Alert, Button } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import StreamsStore from 'stores/streams/StreamsStore';

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
      userReceiver: '',
      emailReceiver: '',
    };
  },
  _onChangeUser(evt) {
    this.setState({userReceiver: evt.target.value});
  },
  _onChangeEmail(evt) {
    this.setState({emailReceiver: evt.target.value});
  },
  _addUserReceiver(evt) {
    evt.preventDefault();
    StreamsStore.addReceiver(this.props.streamId, 'users', this.refs.user.getValue());
    this.setState({userReceiver: ''});
  },
  _addEmailReceiver(evt) {
    evt.preventDefault();
    StreamsStore.addReceiver(this.props.streamId, 'emails', this.refs.email.getValue());
    this.setState({emailReceiver: ''});
  },
  _formatReceiverList(receivers) {
    if (!receivers || (receivers.users.length === 0 && receivers.emails.length === 0)) {
      return <Alert bsStyle="info">No configured alert receivers.</Alert>;
    }

    const userReceivers = this.props.receivers.users ? this.props.receivers.users
      .map((receiver) => <AlertReceiver key={'users-' + receiver} type="users" receiver={receiver} streamId={this.props.streamId}/>) : null;
    const emailReceivers = this.props.receivers.emails ? this.props.receivers.emails
      .map((receiver) => <AlertReceiver key={'email-' + receiver} type="emails" receiver={receiver} streamId={this.props.streamId}/>) : null;
    return (
      <ul className="alert-receivers">
        {userReceivers}
        {emailReceivers}
      </ul>
    );
  },
  render() {
    return (
      <span>
        {this._formatReceiverList(this.props.receivers)}
        {this.isPermitted(this.state.currentUser.permissions, 'streams:edit:' + this.props.streamId) &&
          <Row id="add-alert-receivers" className="row-sm">

            <Col md={6}>
              <form className="form-inline" onSubmit={this._addUserReceiver} >
                <Input ref="user" label="Username:" type="text" autoComplete="off" value={this.state.userReceiver} onChange={this._onChangeUser}/>
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
