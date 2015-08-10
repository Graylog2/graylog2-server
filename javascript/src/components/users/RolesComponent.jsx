'use strict';

var React = require('react');
var Immutable = require('immutable');

var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;

var RoleList = require('./RoleList');
var EditRole = require('./EditRole');

var RolesStore = require('../../stores/users/RolesStore').RolesStore;
var UserNotification = require('../../util/UserNotification');

var RolesComponent = React.createClass({

    getInitialState() {
        return {
            roles: Immutable.Set(),
            rolesLoaded: false,

            editRole: null
        };
    },
    componentDidMount() {
        this.loadRoles();
    },

    loadRoles() {
        var promise = RolesStore.loadRoles();
        promise.done((roles) => {
            this.setState({roles: Immutable.Set(roles), rolesLoaded: true});
        });
    },

    _showEditRole(role) {
        this.setState({editRole: role});
    },
    _deleteRole(role) {
        if (window.confirm("Do you really want to delete role " + role.name + "?")) {
            RolesStore.getMembers(role.name).done((membership) => {
                if (membership.users.length != 0) {
                    UserNotification.error("Cannot delete role " + role.name + ". It is still assigned to " + membership.users.length + " users.");
                } else {
                    RolesStore.deleteRole(role.name);
                }
            });
        }
    },
    _saveRole(initialName, role) {
        RolesStore.updateRole(initialName, role).done(() => {this.setState({editRole: null})});
    },
    _cancelEdit(ev) {
        this.setState({editRole: null});
    },

    render() {
        var content = null;
        if (!this.state.rolesLoaded) {
            content = <span>Loading roles...</span>;
        } else if (this.state.editRole !== null) {
            content = <EditRole initialRole={this.state.editRole} onSave={this._saveRole} cancelEdit={this._cancelEdit}/>;
        } else {
            content = <RoleList roles={this.state.roles}
                                showEditRole={this._showEditRole}
                                deleteRole={this._deleteRole} />;
        }
        return (
            <Row>
                <Col md={12}>
                    {content}
                </Col>
            </Row>
        );
    }
});


module.exports = RolesComponent;