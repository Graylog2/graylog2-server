import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const UsersStore = StoreProvider.getStore('Users');
const StartpageStore = StoreProvider.getStore('Startpage');

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import UserForm from 'components/users/UserForm';

import UserPreferencesButton from 'components/users/UserPreferencesButton';

class EditUsersPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  state = {
    user: undefined,
  };

  componentDidMount() {
    this._loadUser(this.props.params.username);
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.params.username !== nextProps.params.username) {
      this._loadUser(nextProps.params.username);
    }
  }

  _loadUser = (username) => {
    UsersStore.load(username).then((user) => {
      this.setState({ user: user });
    });
  };

  _resetStartpage = () => {
    if (window.confirm('Are you sure you want to reset the start page?')) {
      const username = this.props.params.username;
      StartpageStore.set(username).then(() => this._loadUser(username));
    }
  };

  render() {
    if (!this.state.user) {
      return <Spinner />;
    }

    const user = this.state.user;
    let resetStartpageButton;
    if (!user.read_only && user.startpage !== null && Object.keys(user.startpage).length > 0) {
      resetStartpageButton = <Button bsStyle="info" onClick={this._resetStartpage}>Reset custom startpage</Button>;
    }

    const userPreferencesButton = !user.read_only ?
      (<span id="react-user-preferences-button" data-user-name={this.props.params.username}>
        <UserPreferencesButton userName={user.username} />
      </span>)
      : null;

    return (
      <DocumentTitle title={`Edit user ${this.props.params.username}`}>
        <span>
          <PageHeader title={<span>Edit user <em>{this.props.params.username}</em></span>} subpage>
            <span>You can either change the details of a user here or set a new password.</span>
            {null}
            <div>
              {resetStartpageButton}{' '}
              {userPreferencesButton}
            </div>
          </PageHeader>

          <UserForm user={this.state.user} />
        </span>
      </DocumentTitle>
    );
  }
}

export default EditUsersPage;
