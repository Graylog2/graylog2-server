'use strict';

var React = require('react/addons');
var SourceOverview = require('./SourceOverview');
var Router = require('react-router');
var Route = Router.Route;
var Routes = Router.Routes;
var NotFoundRoute = Router.NotFoundRoute;
var DefaultRoute = Router.DefaultRoute;

// to get the range from the URL
var routes = (
    <Routes location="hash">
        <Route name="app" path="/:range?" handler={SourceOverview}/>
        <DefaultRoute handler={SourceOverview} />
        <NotFoundRoute handler={SourceOverview} />
    </Routes>
);

var sourceOverviewDiv = document.getElementById('react-sources');
if (sourceOverviewDiv) {
    React.renderComponent(routes, sourceOverviewDiv);
}

