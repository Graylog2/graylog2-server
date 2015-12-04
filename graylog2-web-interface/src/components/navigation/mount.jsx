'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var Navigation = require('./Navigation');

var elem = document.getElementById('navigation-bar');
ReactDOM.render(
    <Navigation
        requestPath={elem.getAttribute("data-request-path")}
        permissions={elem.getAttribute("data-permissions")}
        loginName={elem.getAttribute("data-login-name")}
        fullName={elem.getAttribute("data-full-name")}/>, elem);

