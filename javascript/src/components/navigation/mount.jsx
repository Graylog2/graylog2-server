'use strict';

var React = require('react/addons');
//var UserMenu = require('./UserMenu');
var Navigation = require('./Navigation');

var elem = document.getElementById('navigation-bar');
React.render(
    <Navigation
        requestPath={elem.getAttribute("data-request-path")}
        permissions={elem.getAttribute("data-permissions")}
        loginName={elem.getAttribute("data-login-name")}
        fullName={elem.getAttribute("data-full-name")}/>, elem);

