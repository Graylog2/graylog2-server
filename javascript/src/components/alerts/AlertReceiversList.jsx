import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Input, Alert, Button } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import StreamsStore from 'stores/streams/StreamsStore';

import AlertReceiver from 'components/alerts/AlertReceiver';

const AlertReceiversList = React.createClass({
  propTypes: {
    receivers: React.PropTypes.object.isRequired,
    streamId: React.PropTypes.string.isRequired,
  },
  getInitialState() {
    return {
      userReceiver: '',
      emailReceiver: '',
    };
  },
  mixins: [PermissionsMixin, Reflux.connect(CurrentUserStore)],
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
  render() {
    if (this.props.receivers.users.length === 0 && this.props.receivers.emails.length === 0) {
      return <Alert bsStyle="info">No configured alert receivers.</Alert>;
    }

    const userReceivers = this.props.receivers.users.map((receiver) => <AlertReceiver key={'users-' + receiver} type="users"
                                                                                      receiver={receiver} streamId={this.props.streamId}/>);
    const emailReceivers = this.props.receivers.emails.map((receiver) => <AlertReceiver key={'email-' + receiver} type="emails"
                                                                                        receiver={receiver} streamId={this.props.streamId}/>);
    return (
      <span>
        <ul className="alert-receivers">
          {userReceivers}
          {emailReceivers}
        </ul>
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
