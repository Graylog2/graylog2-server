/* global jsRoutes */

'use strict';

var React = require('react');
var NavDropdown = require('react-bootstrap').NavDropdown;
var MenuItem = require('react-bootstrap').MenuItem;

var UserMenu = React.createClass({
    getInitialState() {
        return ({
        });
    },
    componentWillMount() {
    },
    render() {
        return (
            <NavDropdown navItem title={this.props.fullName} id="user-menu-dropdown">
                <MenuItem href={jsRoutes.controllers.UsersController.editUserForm(this.props.loginName).url}>Edit profile</MenuItem>
                <MenuItem divider />
                <MenuItem href={jsRoutes.controllers.SessionsController.destroy().url}><i className="fa fa-sign-out"></i> Log out</MenuItem>
            </NavDropdown>
        );
    }
});

module.exports = UserMenu;
