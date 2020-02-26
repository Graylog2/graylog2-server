import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import TokenList from 'components/users/TokenList';
import StoreProvider from 'injection/StoreProvider';
import { DocumentTitle, PageHeader } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';

const UsersStore = StoreProvider.getStore('Users');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const EditTokensPage = createReactClass({
  displayName: 'EditTokensPage',
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      username: undefined,
      creatingToken: false,
      deletingToken: undefined,
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

  _deleteToken(tokenId, tokenName) {
    const promise = UsersStore.deleteToken(this.props.params.username, tokenId, tokenName);
    this.setState({ deletingToken: tokenId });
    promise.then(() => {
      this._loadTokens(this.props.params.username);
      this.setState({ deletingToken: undefined });
    });
  },

  _createToken(tokenName) {
    const promise = UsersStore.createToken(this.props.params.username, tokenName);
    this.setState({ creatingToken: true });
    promise.then(() => {
      this._loadTokens(this.props.params.username);
      this.setState({ creatingToken: false });
    });
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
                     onDelete={this._deleteToken}
                     onCreate={this._createToken}
                     creatingToken={this.state.creatingToken}
                     deletingToken={this.state.deletingToken} />
        </span>
      </DocumentTitle>
    );
  },
});

export default EditTokensPage;
