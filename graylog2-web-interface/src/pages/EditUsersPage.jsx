import React, {PropTypes} from 'react';
import {Button} from 'react-bootstrap';

import UsersStore from 'stores/users/UsersStore';
import StartpageStore from 'stores/users/StartpageStore';

import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';
import UserForm from 'components/users/UserForm';

import UserPreferencesButton from 'components/users/UserPreferencesButton';

const EditUsersPage = React.createClass({
  propTypes: {
    username: PropTypes.string.isRequired,
  },
  getInitialState() {
    return {
      user: undefined,
    };
  },
  componentDidMount() {
    this._loadUser();
  },
  _loadUser() {
    UsersStore.load(this.props.params.username).then((user) => {
      this.setState({user: user});
    });
  },
  _resetStartpage() {
    if (window.confirm('Are you sure you want to reset your current start page?')) {
      StartpageStore.set(this.props.params.username).then(() => this._loadUser());
    }
  },
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
      <span id="react-user-preferences-button" data-user-name={this.props.params.username}>
        <UserPreferencesButton userName={user.username}/>
      </span>
      : null;

    return (
      <span>
        <PageHeader title={'Edit user »' + this.props.params.username + '«'} titleSize={8} buttonSize={4} buttonStyle={{textAlign: 'right', marginTop: '10px'}}>
          <span>You can either change the details of a user here or set a new password.</span>
          {null}
          <div>
            {resetStartpageButton}{' '}
            {userPreferencesButton}
          </div>
        </PageHeader>

        <UserForm user={this.state.user} />
      </span>
    );
  },
});

export default EditUsersPage;
