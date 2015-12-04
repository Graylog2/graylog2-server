import React from 'react';

import UsersStore from 'stores/users/UsersStore';

import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';
import UserForm from 'components/users/UserForm';

import UserPreferencesModal from 'components/users/UserPreferencesModal';
import UserPreferencesButton from 'components/users/UserPreferencesButton';

const EditUsersPage = React.createClass({
  getInitialState() {
    return {
      user: undefined,
    };
  },
  componentDidMount() {
    UsersStore.load(this.props.params.username).then((user) => {
      this.setState({user: user});
    });
  },
  render() {
    if (!this.state.user) {
      return <Spinner />;
    }

    const user = this.state.user;
    const resetStartpageButton = (!user.read_only && user.startpage !== null && Object.keys(user.startpage).length > 0) ?
      <button type="submit" className="btn btn-info" data-confirm="Really reset startpage?">
        Reset custom startpage
      </button>
      : null;
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
    )
  },
});

export default EditUsersPage;
