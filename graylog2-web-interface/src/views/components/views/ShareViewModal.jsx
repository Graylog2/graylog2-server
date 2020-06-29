// @flow strict
import * as React from 'react';
import { get } from 'lodash';
import PropTypes from 'prop-types';

import UserNotification from 'util/UserNotification';
import { FormGroup, HelpBlock, Radio } from 'components/graylog';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';
import Select from 'views/components/Select';
import Spinner from 'components/common/Spinner';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import StoreProvider from 'injection/StoreProvider';
import View from 'views/logic/views/View';
import { ViewSharingActions } from 'views/stores/ViewSharingStore';
import ViewSharing from 'views/logic/views/sharing/ViewSharing';
import AllUsersOfInstance from 'views/logic/views/sharing/AllUsersOfInstance';
import SpecificRoles from 'views/logic/views/sharing/SpecificRoles';
import SpecificUsers from 'views/logic/views/sharing/SpecificUsers';
import UserShortSummary from 'views/logic/views/sharing/UserShortSummary';
import type { User } from 'stores/users/UsersStore';

const RolesStore = StoreProvider.getStore('Roles');

type Props = {
  currentUser: ?User,
  onClose: (?ViewSharing) => void,
  view: View,
  show: boolean,
};

type State = {
  viewSharing: ViewSharing | null,
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
  static propTypes = {
    currentUser: PropTypes.object,
    onClose: PropTypes.func.isRequired,
    view: PropTypes.instanceOf(View).isRequired,
    show: PropTypes.bool.isRequired,
  }

  static defaultProps = {
    currentUser: undefined,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      viewSharing: null,
      loaded: false,
      users: undefined,
      roles: undefined,
    };
  }

  componentDidMount() {
    const { view, currentUser } = this.props;

    Promise.all([
      ViewSharingActions.get(view.id),
      ViewSharingActions.users(view.id),
      this._isAdmin(currentUser) ? RolesStore.loadRoles().then((roles) => roles.map(({ name }) => name)) : Promise.resolve(currentUser?.roles),
    ]).then(([viewSharing, users, roles]) => {
      this.setState({ viewSharing, users, roles, loaded: true });
    });
  }

  _onSave = () => {
    const { view } = this.props;
    const { viewSharing } = this.state;
    const viewTypeLabel = ViewTypeLabel({ type: view.type });
    let promise;

    if (viewSharing) {
      promise = ViewSharingActions.create(view.id, viewSharing);
    } else {
      promise = ViewSharingActions.remove(view.id);
    }

    const { onClose } = this.props;

    promise.then(() => {
      onClose(viewSharing);
      UserNotification.success(`Sharing ${viewTypeLabel} "${view.title}" was successful!`, 'Success!');
    }).catch((error) => {
      UserNotification.error(`Sharing ${viewTypeLabel} failed: ${error?.additional?.body?.message ?? error}`, 'Error!');
    });
  };

  // eslint-disable-next-line react/destructuring-assignment
  _onClose = () => this.props.onClose();

  _onChange = (e: SyntheticInputEvent<HTMLInputElement>) => {
    const { view } = this.props;
    const type = e.target.name;
    const viewSharing = type === 'none' ? null : ViewSharing.fromJSON({ type, view_id: view.id });

    this.setState({ viewSharing });
  };

  _onRolesChange = (newRoles: Array<{value: string, label: string}>) => {
    const { viewSharing } = this.state;

    if (viewSharing === null || viewSharing.type !== SpecificRoles.Type) {
      return;
    }

    // $FlowFixMe: At this point we have a SpecificRoles instance.
    const specificRoles: SpecificRoles = viewSharing;
    const roles = newRoles.map(extractValue);

    this.setState({ viewSharing: specificRoles.toBuilder().roles(roles).build() });
  };

  _onUsersChange = (newUsers: Array<{value: string, label: string}>) => {
    const { viewSharing } = this.state;

    if (viewSharing === null || viewSharing.type !== SpecificUsers.Type) {
      return;
    }

    // $FlowFixMe: At this point we have a SpecificUsers instance.
    const specificUsers: SpecificUsers = viewSharing;
    const users = newUsers.map(extractValue);

    this.setState({ viewSharing: specificUsers.toBuilder().users(users).build() });
  };

  _isAdmin = (user: ?User) => (user?.roles.includes('Admin') || user?.permissions.includes('*'));

  render() {
    const { show, view } = this.props;
    const { loaded, viewSharing, roles, users } = this.state;
    const type = get(viewSharing, 'type', 'none');
    const userOptions = users ? users
      // eslint-disable-next-line camelcase
      .map(({ username, fullname }) => ({ value: username, label: fullname })) : null;
    const userValue = get(viewSharing, 'users', []).map((user) => (userOptions || []).find((option) => option.value === user));
    const rolesOptions = roles ? roles.map((rolename) => ({ value: rolename, label: rolename })) : null;
    const rolesValue = get(viewSharing, 'roles', []).map((role) => ({ label: role, value: role }));
    const viewTypeLabel = <ViewTypeLabel type={view.type} />;
    const content = !loaded ? <Spinner /> : (
      <FormGroup style={formStyle}>
        <Radio name={AllUsersOfInstance.Type} checked={type === AllUsersOfInstance.Type} onChange={this._onChange}>
          Any user of this Graylog
        </Radio>{' '}
        <Additional>
          <HelpBlock>Anyone with an account can access the {viewTypeLabel}.</HelpBlock>
        </Additional>

        <Radio name={SpecificRoles.Type} checked={type === SpecificRoles.Type} onChange={this._onChange}>
          Specific roles:{' '}
        </Radio>{' '}
        <Additional>
          <Select isMulti
                  isDisabled={type !== SpecificRoles.Type}
                  value={rolesValue}
                  placeholder="Select roles"
                  stripDiacritics
                  onChange={this._onRolesChange}
                  options={rolesOptions} />
          <HelpBlock>Only users with these roles can access the {viewTypeLabel}.</HelpBlock>
        </Additional>

        <Radio name={SpecificUsers.Type} checked={type === SpecificUsers.Type} onChange={this._onChange}>
          Specific users:
        </Radio>
        <Additional>
          <Select isMulti
                  isDisabled={type !== SpecificUsers.Type}
                  value={userValue}
                  placeholder="Select users"
                  stripDiacritics
                  onChange={this._onUsersChange}
                  options={userOptions || []} />
          <HelpBlock>Only these users can access the {viewTypeLabel}.</HelpBlock>
        </Additional>

        <Radio name="none" checked={type === 'none'} onChange={this._onChange}>
          Only me
        </Radio>
        <Additional>
          <HelpBlock>Noone but you can access the {viewTypeLabel}.</HelpBlock>
        </Additional>
      </FormGroup>
    );

    return (
      <BootstrapModalConfirm onCancel={() => this._onClose()}
                             onConfirm={() => this._onSave()}
                             title={<>Who is supposed to access the {viewTypeLabel} {view.title}</>}
                             confirmButtonText="Save"
                             showModal={show}>
        {content}
      </BootstrapModalConfirm>
    );
  }
}

export default ShareViewModal;
