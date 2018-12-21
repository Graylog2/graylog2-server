// @flow strict
import * as React from 'react';
import { Button, FormGroup, HelpBlock, Modal, Radio } from 'react-bootstrap';
import { get } from 'lodash';
import Select from 'react-select';

// $FlowFixMe: imports from core need to be fixed in flow
import Spinner from 'components/common/Spinner';
// $FlowFixMe: imports from core need to be fixed in flow
import StoreProvider from 'injection/StoreProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

import View from 'enterprise/logic/views/View';
import { ViewSharingActions } from 'enterprise/stores/ViewSharingStore';
import ViewSharing from 'enterprise/logic/views/sharing/ViewSharing';
import AllUsersOfInstance from 'enterprise/logic/views/sharing/AllUsersOfInstance';
import SpecificRoles from 'enterprise/logic/views/sharing/SpecificRoles';
import SpecificUsers from 'enterprise/logic/views/sharing/SpecificUsers';
import UserShortSummary from 'enterprise/logic/views/sharing/UserShortSummary';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const RolesStore = StoreProvider.getStore('Roles');

type Props = {
  currentUser: {
    username: string,
    roles: Array<string>,
    permissions: Array<string>,
  },
  onClose: (?ViewSharing) => void,
  view: View,
  show: boolean,
};

type User = {
  roles: Array<string>,
  permissions: Array<string>,
};

type State = {
  viewSharing: ViewSharing,
  loaded: boolean,
  users?: Array<UserShortSummary>,
  roles?: Array<string>,
};

const formStyle = {
  margin: 'auto',
  width: 'fit-content',
  padding: '2rem',
};

const Additional = ({ children }: { children: React.Node }) => <div style={{ paddingLeft: '2rem' }}>{children}</div>;

const extractValue = ({ value }) => value;

class ShareViewModal extends React.Component<Props, State> {
  state = {
    viewSharing: null,
    loaded: false,
    users: undefined,
    roles: undefined,
  };

  componentDidMount() {
    const { view, currentUser } = this.props;
    Promise.all([
      ViewSharingActions.get(view.id),
      ViewSharingActions.users(view.id),
      this._isAdmin(currentUser) ? RolesStore.loadRoles().then(roles => roles.map(({ name }) => name)) : Promise.resolve(currentUser.roles),
    ]).then(([viewSharing, users, roles]) => {
      this.setState({ viewSharing, users, roles, loaded: true });
    });
  }

  _onSave = () => {
    const { view } = this.props;
    const { viewSharing } = this.state;
    let promise;
    if (viewSharing) {
      promise = ViewSharingActions.create(view.id, viewSharing);
    } else {
      promise = ViewSharingActions.remove(view.id);
    }
    promise.then(() => this.props.onClose(viewSharing));
  };
  _onClose = () => this.props.onClose();

  // eslint-disable-next-line no-undef
  _onChange = (e: SyntheticInputEvent<HTMLInputElement>) => {
    const { view } = this.props;
    const type = e.target.name;
    const viewSharing = type === 'none' ? null : ViewSharing.fromJSON({ type, view_id: view.id });
    this.setState({ viewSharing });
  };

  _onRolesChange = (newRoles) => {
    const { viewSharing }: { viewSharing: SpecificRoles } = this.state;
    const roles = newRoles.map(extractValue);
    this.setState({ viewSharing: viewSharing.toBuilder().roles(roles).build() });
  };

  _onUsersChange = (newUsers) => {
    const { viewSharing }: { viewSharing: SpecificUsers } = this.state;
    const users = newUsers.map(extractValue);
    this.setState({ viewSharing: viewSharing.toBuilder().users(users).build() });
  };

  _isAdmin = (user: User) => (user.roles.includes('Admin') || user.permissions.includes('*'));

  render() {
    const { show, view } = this.props;
    const { loaded, viewSharing, roles, users } = this.state;
    const type = get(viewSharing, 'type', 'none');
    const userOptions = users ? users
      // eslint-disable-next-line camelcase
      .map(({ username, fullname }) => ({ value: username, label: fullname })) : null;
    const rolesOptions = roles ? roles.map(rolename => ({ value: rolename, label: rolename })) : null;
    const content = !loaded ? <Spinner /> : (
      <FormGroup style={formStyle}>
        <Radio name={AllUsersOfInstance.Type} checked={type === AllUsersOfInstance.Type} onChange={this._onChange}>
          Any user of this Graylog
        </Radio>{' '}
        <Additional>
          <HelpBlock>Anyone with an account can access the view.</HelpBlock>
        </Additional>

        <Radio name={SpecificRoles.Type} checked={type === SpecificRoles.Type} onChange={this._onChange}>
          Specific roles:{' '}
        </Radio>{' '}
        <Additional>
          <Select multi
                  disabled={type !== SpecificRoles.Type}
                  value={get(viewSharing, 'roles', [])}
                  placeholder="Select roles"
                  onChange={this._onRolesChange}
                  options={rolesOptions} />
          <HelpBlock>Only users with these roles can access the view.</HelpBlock>
        </Additional>

        <Radio name={SpecificUsers.Type} checked={type === SpecificUsers.Type} onChange={this._onChange}>
          Specific users:
        </Radio>
        <Additional>
          <Select multi
                  disabled={type !== SpecificUsers.Type}
                  value={get(viewSharing, 'users', [])}
                  placeholder="Select users"
                  onChange={this._onUsersChange}
                  options={userOptions} />
          <HelpBlock>Only these users can access the view.</HelpBlock>
        </Additional>

        <Radio name="none" checked={type === 'none'} onChange={this._onChange}>
          Only me
        </Radio>
        <Additional>
          <HelpBlock>Noone but you can access the view.</HelpBlock>
        </Additional>
      </FormGroup>
    );
    return (
      <Modal show={show} onHide={this._onClose}>
        <Modal.Body>
          <h3>Who is supposed to access the view {view.title}?</h3>
          {content}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this._onSave} bsStyle="success">Save</Button>
          <Button onClick={this._onClose}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

export default connect(ShareViewModal, { currentUser: CurrentUserStore }, ({ currentUser, ...rest }) => Object.assign({}, rest, { currentUser: currentUser.currentUser }));
