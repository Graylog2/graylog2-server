'use strict';

var React = require('react/addons');
var SourceOverview = require('./SourceOverview');
var Router = require('react-router');
var Route = Router.Route;
var NotFoundRoute = Router.NotFoundRoute;
var DefaultRoute = Router.DefaultRoute;

// to get the range from the URL
var routes = (
    <Route path="/" handler={SourceOverview}>
        <Route name="app" path="/:range?" handler={SourceOverview}/>
        <DefaultRoute handler={SourceOverview} />
        <NotFoundRoute handler={SourceOverview} />
    </Route>
);

var sourceOverviewDiv = document.getElementById('react-sources');
if (sourceOverviewDiv) {
    Router.run(routes, function(Handler, state) {
      React.render(<Handler params={state.params} query={state.query}/>, sourceOverviewDiv);
    });
}
