import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Alert, Button, Col, Row, Table } from 'react-bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { DocumentTitle, IfPermitted, PageHeader, SortableList } from 'components/common';
import Routes from 'routing/Routes';
import ObjectUtils from 'util/ObjectUtils';
import history from 'util/History';
import naturalSort from 'javascript-natural-sort';

const AuthProvidersConfig = createReactClass({
  displayName: 'AuthProvidersConfig',

  propTypes: {
    config: PropTypes.object.isRequired,
    descriptors: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        disabled_realms: [],
        realm_order: [],
      },
    };
  },

  getInitialState() {
    return {
      config: {
        disabled_realms: this.props.config.disabled_realms,
        realm_order: this.props.config.realm_order,
      },
    };
  },

  inputs: {},

  _openModal() {
    this.configModal.open();
  },

  _closeModal() {
    this.configModal.close();
  },

  _saveConfig() {
    if (!this._hasNoActiveRealm()) {
      this.props.updateConfig(this.state.config).then(() => {
        this._closeModal();
      });
    }
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _onCancel() {
    history.push(Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
  },

  _updateSorting(newSorting) {
    const update = ObjectUtils.clone(this.state.config);

    update.realm_order = newSorting.map(entry => entry.id);

    this.setState({ config: update });
  },

  _toggleStatus(realmName) {
    return () => {
      const disabledProcessors = this.state.config.disabled_realms;
      const update = ObjectUtils.clone(this.state.config);
      const checked = this.inputs[realmName].checked;

      if (checked) {
        update.disabled_realms = disabledProcessors.filter(p => p !== realmName);
      } else if (disabledProcessors.indexOf(realmName) === -1) {
        update.disabled_realms.push(realmName);
      }

      this.setState({ config: update });
    };
  },

  _hasNoActiveRealm() {
    return this.state.config.disabled_realms.length >= this.state.config.realm_order.length;
  },

  _noActiveRealmWarning() {
    if (this._hasNoActiveRealm()) {
      return (
        <Alert bsStyle="danger">
          <strong>ERROR:</strong> No active authentication provider!
        </Alert>
      );
    }
    return null;
  },

  _summary() {
    return this.state.config.realm_order.map((name, idx) => {
      const status = this.state.config.disabled_realms.filter(disabledName => disabledName === name).length > 0 ? 'disabled' : 'active';
      const realm = (this.props.descriptors[name] || { id: name, title: 'Unavailable' });
      return (
        <tr key={idx}>
          <td>{idx + 1}</td>
          <td>{realm.displayName}</td>
          <td>{realm.description}</td>
          <td>{status}</td>
        </tr>
      );
    });
  },

  _sortableItems() {
    return this.state.config.realm_order.map((name) => {
      const realm = (this.props.descriptors[name] || { id: name, title: 'Unavailable' });
      return { id: realm.name, title: realm.displayName };
    });
  },

  _statusForm() {
    return ObjectUtils.clone(this.state.config.realm_order).sort((a, b) => naturalSort(a.displayName, b.displayName)).map((realmName, idx) => {
      const enabled = this.state.config.disabled_realms.filter(disabledName => disabledName === realmName).length < 1;
      const realm = (this.props.descriptors[realmName] || { id: realmName, displayName: 'Unavailable' });

      return (
        <tr key={idx}>
          <td>{realm.displayName}</td>
          <td>
            <input ref={(elem) => { this.inputs[realm.name] = elem; }}
                   type="checkbox"
                   checked={enabled}
                   disabled={!realm.canBeDisabled}
                   onChange={this._toggleStatus(realm.name)} />
          </td>
        </tr>
      );
    });
  },

  render() {
    return (
      <DocumentTitle title="Authentication Providers">
        <span>
          <PageHeader title="Authentication Providers" subpage>
            <span>The following authentication providers executed in order during login. Disabled providers will be
              skipped.<br />A user is authenticated by the first matching provider, a successful match can cause a Graylog account for
              this user to be created.
            </span>
          </PageHeader>
          <Row>
            <IfPermitted permissions={['clusterconfigentry:read', 'authentication:read']}>
              <Col md={6}>
                <Table striped bordered className="top-margin">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Provider</th>
                      <th>Description</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {this._summary()}
                  </tbody>
                </Table>

                <IfPermitted permissions={['clusterconfigentry:edit', 'authentication:edit']}>
                  <Button bsStyle="primary" onClick={this._openModal} className="save-button-margin">Edit</Button>
                  <Button onClick={this._onCancel}>Cancel</Button>
                </IfPermitted>

                <BootstrapModalForm ref={(configModal) => { this.configModal = configModal; }}
                                    title="Update Authentication Provider Configuration"
                                    onSubmitForm={this._saveConfig}
                                    onModalClose={this._resetConfig}
                                    submitButtonText="Save">
                  <h3>Order</h3>
                  <p>Use drag and drop to change the execution order of the authentication providers.</p>
                  <SortableList items={this._sortableItems()} onMoveItem={this._updateSorting} />

                  <h3>Status</h3>
                  <p>Change the checkboxes to change the status of an authentication provider.</p>
                  <Table striped bordered condensed className="top-margin">
                    <thead>
                      <tr>
                        <th>Provider</th>
                        <th>Enabled</th>
                      </tr>
                    </thead>
                    <tbody>
                      {this._statusForm()}
                    </tbody>
                  </Table>
                  {this._noActiveRealmWarning()}
                </BootstrapModalForm>
              </Col>
            </IfPermitted>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default AuthProvidersConfig;
