import React from 'react';
import Navigation from 'components/navigation/Navigation';
import { Row, Col } from 'react-bootstrap';

import 'javascripts/shims/styles/shim.css';
import 'stylesheets/bootstrap.min.css';
import 'stylesheets/font-awesome.min.css';
import 'stylesheets/newfonts.less';
import 'stylesheets/bootstrap-submenus.less';
import 'stylesheets/rickshaw.min.css';
import 'stylesheets/toastr.min.css';
import 'stylesheets/datepicker.less';
import 'stylesheets/chosen.bootstrap.min.css';
import 'stylesheets/chosen-bootstrap.less';
import 'stylesheets/jquery.gridster.min.css';
import 'stylesheets/jquery.dynatable.css';
import 'stylesheets/typeahead.less';
import 'stylesheets/graylog2.less';
import 'c3/c3.css';
import 'dc/dc.css';

const App = React.createClass({
  render() {
    return (
      <div>
        <Navigation requestPath="/" fullName="John Doe" loginName="johndoe"/>
        <div className="container-fluid">
          <Row id="main-row">
            <Col md={12} id="main-content">
              {this.props.children}
            </Col>
          </Row>
        </div>
      </div>
    );
  },
});

export default App;
