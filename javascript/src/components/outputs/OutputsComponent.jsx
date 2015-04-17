'use strict';

var React = require('react/addons');
var OutputList = require('./OutputList');
var CreateOutputDropdown = require('./CreateOutputDropdown');

var OutputComponent = React.createClass({
    _isPermitted(permissions) {
        var result = permissions.every((p) => this.state.permissions[p]);
        return result;
    },
    getInitialState() {
      return {
          permissions: JSON.parse(this.props.permissions)
      };
    },
    willReceiveProps(props) {
        this.setState(props);
    },
    handleUpdate() {
        this.refs.outputList.loadData();
    },
    render() {
        var permissions = this.state.permissions;
        var createOutputDropdown = (this._isPermitted(["OUTPUTS_CREATE"]) ? <CreateOutputDropdown onUpdate={this.handleUpdate}/> : "");
        return (<div className="outputs">
                    <div className="row input-new content">
                        <div className="col-md-12">
                            {createOutputDropdown}
                        </div>
                    </div>

                    <OutputList ref="outputList" permissions={permissions}/>
                </div>);
    }
});
module.exports = OutputComponent;

