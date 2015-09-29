import React from 'react';
import App from 'routing/App';
import { Router, Route } from 'react-router';
import StreamsPage from 'components/streams/StreamsPage';
import Routes from 'routing/Routes';

import 'stylesheets/graylog2.less';
import 'stylesheets/bootstrap-submenus.less';
import 'stylesheets/rickshaw.min.css';
import 'stylesheets/toastr.min.css';
import 'stylesheets/datepicker.less';
import 'stylesheets/chosen.bootstrap.min.css';
import 'stylesheets/chosen-bootstrap.less';
import 'stylesheets/jquery.gridster.min.css';
import 'stylesheets/jquery.dynatable.css';
import 'stylesheets/typeahead.less';
import 'c3/c3.css';
import 'dc/dc.css';

const AppRouter = React.createClass({
  render() {
    return (
      <Router>
        <Route path="/" component={App}>
          <Route path={Routes.STREAMS} component={StreamsPage}/>
        </Route>
      </Router>
    );
  },
});

export default AppRouter;
