import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import Navigation from 'components/navigation/Navigation';
import Spinner from 'components/common/Spinner';

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

import CurrentUserStore from 'stores/users/CurrentUserStore';

const App = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    if (!this.state.currentUser) {
      return <Spinner />;
    }
    return (
      <div>
        <Navigation requestPath="/" fullName={this.state.currentUser.full_name}
                    loginName={this.state.currentUser.username}/>
        <div id="scroll-to-hint" style={{display: 'none'}} className="alpha80">
          <i className="fa fa-arrow-up"></i>
        </div>
        {this.props.children}
      </div>
    );
  },
});

export default App;
