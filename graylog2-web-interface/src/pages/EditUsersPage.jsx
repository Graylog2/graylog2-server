import PropTypes from 'prop-types';
import React from 'react';

import StoreProvider from 'injection/StoreProvider';
import { Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import UserForm from 'components/users/UserForm';
import UserPreferencesButton from 'components/users/UserPreferencesButton';

const UsersStore = StoreProvider.getStore('Users');
const StartpageStore = StoreProvider.getStore('Startpage');

class EditUsersPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  state = {
    user: undefined,
  };

  componentDidMount() {
    const { params } = this.props;

    this._loadUser(params.username);
  }

  componentWillReceiveProps(nextProps) {
    const { params } = this.props;

    if (params.username !== nextProps.params.username) {
      this._loadUser(nextProps.params.username);
    }
  }

  _loadUser = (username) => {
    UsersStore.load(username).then((user) => {
      this.setState({ user: user });
    });
  };

  _resetStartpage = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm('Are you sure you want to reset the start page?')) {
      const { params: { username } } = this.props;
      StartpageStore.set(username).then(() => this._loadUser(username));
    }
  };

  render() {
    const { user } = this.state;
    const { params } = this.props;

    if (!user) {
      return <Spinner />;
    }

    let resetStartpageButton;
    if (!user.read_only && user.startpage !== null && Object.keys(user.startpage).length > 0) {
      resetStartpageButton = <Button bsStyle="info" onClick={this._resetStartpage}>Reset custom startpage</Button>;
    }

    const userPreferencesButton = !user.read_only
      ? (
        <span id="react-user-preferences-button" data-user-name={params.username}>
          <UserPreferencesButton userName={user.username} />
        </span>
      )
      : null;

    return (
      <DocumentTitle title={`Edit user ${params.username}`}>
        <span>
          <PageHeader title={<span>Edit user <em>{params.username}</em></span>} subpage>
            <span>You can either change the details of a user here or set a new password.</span>
            {null}
            <div>
              {resetStartpageButton}{' '}
              {userPreferencesButton}
            </div>
          </PageHeader>

          <UserForm user={user} />
        </span>
      </DocumentTitle>
    );
  }
}

export default EditUsersPage;
