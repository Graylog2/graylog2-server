import PropTypes from 'prop-types';
import React from 'react';
import Reflux from 'reflux';

import TokenList from 'components/users/TokenList';
import StoreProvider from 'injection/StoreProvider';
import { DocumentTitle, PageHeader } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';

const UsersStore = StoreProvider.getStore('Users');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const EditTokensPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  propTypes: {
    params: PropTypes.object.isRequiered,
  },

  getInitialState() {
    return {
      username: undefined,
      tokens: [],
    };
  },

  componentDidMount() {
    this._loadTokens(this.props.params.username);
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.params.username !== nextProps.params.username) {
      this._loadTokens(nextProps.params.username);
    }
  },

  _loadTokens(username) {
    if (this._canListTokens(username)) {
      UsersStore.loadTokens(username).then((tokens) => {
        this.setState({ tokens: tokens });
      });
    } else {
      this.setState({ tokens: [] });
    }
  },

  _canListTokens(username) {
    return this.isPermitted(this.state.currentUser.permissions,
      [`users:tokenlist:${username}`]);
  },

  _deleteToken(token) {
    const promise = UsersStore.deleteToken(this.props.params.username, token);
    promise.then(() => this._loadTokens(this.props.params.username));
  },

  _createToken(tokenName) {
    const promise = UsersStore.createToken(this.props.params.username, tokenName);
    promise.then(() => this._loadTokens(this.props.params.username));
  },

  render() {
    return (
      <DocumentTitle title={`Edit tokens of user ${this.props.params.username}`}>
        <span>
          <PageHeader title={<span>Edit tokens of user <em>{this.props.params.username}</em></span>} subpage>
            <span>You can create new tokens or delete old ones.</span>
            {null}
          </PageHeader>
          <TokenList tokens={this.state.tokens}
                     delete={this._deleteToken}
                     create={this._createToken} />
        </span>
      </DocumentTitle>
    );
  },
});

export default EditTokensPage;
