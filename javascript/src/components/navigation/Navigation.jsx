/* global jsRoutes */

'use strict';

var React = require('react');
var Navbar = require('react-bootstrap').Navbar;
var CollapsableNav = require('react-bootstrap').CollapsableNav;
var Nav = require('react-bootstrap').Nav;
var NavItem = require('react-bootstrap').NavItem;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;

var GlobalThroughput = require('../throughput/GlobalThroughput');
var UserMenu = require('./UserMenu');

var Navigation = React.createClass({
    getInitialState() {
        return ({
            permissions: {}
        });
    },

    componentWillMount() {
        this.setState({permissions: JSON.parse(this.props.permissions)});
    },

    render() {
        var logoUrl = jsRoutes.controllers.Assets.at("images/toplogo.png").url;
        var homeUrl = jsRoutes.controllers.SearchController.globalSearch().url;
        var brand = (<a href={homeUrl}>
            <img src={logoUrl}/>
        </a>);
        return (
            <Navbar brand={brand} inverse fluid fixedTop toggleNavKey={0}>
                <CollapsableNav eventKey={0}>
                    <Nav navbar>
                        {this._isPermitted(['SEARCHES_ABSOLUTE', 'SEARCHES_RELATIVE', 'SEARCHES_KEYWORD']) &&
                            <NavItem href={jsRoutes.controllers.SearchController.globalSearch().url}
                                     active={this.props.requestPath === '/' || this._isActive("/search")}>Search</NavItem>
                        }
                        {this._isPermitted(['SEARCHES_ABSOLUTE', 'SEARCHES_RELATIVE', 'SEARCHES_KEYWORD']) &&
                        <NavItem href={jsRoutes.controllers.SearchControllerV2.index().url}
                                 active={this._isActive("/searchv2")}>Search V2</NavItem>
                        }
                        <NavItem href={jsRoutes.controllers.StreamsController.index().url}
                                 active={this._isActive("/streams")}>Streams</NavItem>

                        <NavItem href={jsRoutes.controllers.DashboardsController.index().url}
                                 active={this._isActive("/dashboards")}>Dashboards</NavItem>

                        {this.state.permissions['SOURCES_READ'] &&
                            <NavItem href={jsRoutes.controllers.SourcesController.list().url}
                                       active={this._isActive("/sources")}>Sources</NavItem>
                        }
                        <DropdownButton title={this._systemTitle()} active={this._isActive("/system")}>
                            <MenuItem href={jsRoutes.controllers.SystemController.index(0).url}>Overview</MenuItem>
                            <MenuItem href={jsRoutes.controllers.NodesController.nodes().url}>Nodes</MenuItem>
                            { this._isPermitted(['INPUTS_READ']) && <MenuItem href={jsRoutes.controllers.InputsController.index().url}>Inputs</MenuItem> }
                            { this._isPermitted(['OUTPUTS_READ']) && <MenuItem href={jsRoutes.controllers.OutputsController.index().url}>Outputs</MenuItem> }
                            { this._isPermitted(['COLLECTORS_READ']) && <MenuItem href={jsRoutes.controllers.CollectorsController.index().url}>Collectors</MenuItem> }
                            { this._isPermitted(['INDICES_READ']) && <MenuItem href={jsRoutes.controllers.IndicesController.index().url}>Indices</MenuItem> }
                            { this._isPermitted(['LOGGERS_READ']) && <MenuItem href={jsRoutes.controllers.LoggingController.index().url}>Logging</MenuItem> }
                            { this._isPermitted(['USERS_EDIT']) && <MenuItem href={jsRoutes.controllers.UsersController.index().url}>Users</MenuItem> }
                            { this._isPermitted(['DASHBOARDS_CREATE', 'INPUTS_CREATE', 'STREAMS_CREATE']) && <MenuItem href={jsRoutes.controllers.BundlesController.index().url}>Content Packs</MenuItem> }
                            { this._isPermitted(['INPUTS_EDIT']) && <MenuItem href={jsRoutes.controllers.GrokPatternsController.index().url}>Grok Patterns</MenuItem> }
                        </DropdownButton>
                    </Nav>

                    <Nav navbar>
                        <NavItem href={jsRoutes.controllers.SystemController.index(0).url} className="notification-badge-link">
                            <span className="badge" style={{backgroundColor: '#ff3b00'}} id="notification-badge"></span>
                        </NavItem>
                    </Nav>

                    <Nav navbar right>
                        <NavItem href={jsRoutes.controllers.NodesController.nodes().url}>
                            <GlobalThroughput />
                        </NavItem>
                        <UserMenu fullName={this.props.fullName} loginName={this.props.loginName}/>
                    </Nav>
                </CollapsableNav>
            </Navbar>
        );
    },

    _isActive(prefix) {
        return this.props.requestPath.indexOf(prefix) === 0;
    },

    _systemTitle() {
        var suffix = "";

        if (this._isActive("/system") || this._isActive("/system?page=")) {
            suffix = " / Overview";
        }
        if (this._isActive("/system/nodes") || this._isActive("/system/radios")) {
            suffix = " / Nodes";
        }
        if (this._isActive("/system/inputs")) {
            suffix = " / Inputs";
        }
        if (this._isActive("/system/outputs")) {
            suffix = " / Outputs";
        }
        if (this._isActive("/system/indices")) {
            suffix = " / Indices";
        }
        if (this._isActive("/system/logging")) {
            suffix = " / Logging";
        }
        if (this._isActive("/system/users")) {
            suffix = " / Users";
        }
        if (this._isActive("/system/contentpacks")) {
            suffix = " / Content Packs";
        }
        if (this._isActive("/system/grokpatterns")) {
            suffix = " / Grok Patterns";
        }
        if (this._isActive("/system/collectors")) {
            suffix = " / Collectors";
        }

        return "System" + suffix;
    },
    _isPermitted(permissions) {
        return permissions.every((p) => this.state.permissions[p]);
    }
});

module.exports = Navigation;
