'use strict';

var React = require('react/addons');
var OutputList = require('./OutputList');
var CreateOutputDropdown = require('./CreateOutputDropdown');
var AssignOutputDropdown = require('./AssignOutputDropdown');

var OutputComponent = React.createClass({
    _isPermitted(permissions) {
        var result = permissions.every((p) => this.state.permissions[p]);
        return result;
    },
    getInitialState() {
      return {
          permissions: JSON.parse(this.props.permissions),
          streamId: this.props.streamId
      };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    handleUpdate() {
        if (this.refs.outputList)
            this.refs.outputList.loadData();
        if (this.refs.assignOutputDropdown)
            this.refs.assignOutputDropdown.loadData();
    },
    render() {
        var permissions = this.state.permissions;
        var streamId = this.state.streamId;
        var createOutputDropdown = (this._isPermitted(["OUTPUTS_CREATE"]) ? <CreateOutputDropdown onUpdate={this.handleUpdate} streamId={streamId}/> : "");
        var assignOutputDropdown = (streamId ? <AssignOutputDropdown ref="assignOutputDropdown" streamId={streamId} onUpdate={this.handleUpdate}/> : "");
        return (<div className="outputs">
                    <div className="row input-new content">
                        <div className="col-md-12">
                            {createOutputDropdown}
                            {assignOutputDropdown}
                        </div>
                    </div>

                    <OutputList ref="outputList" streamId={streamId} permissions={permissions} onUpdate={this.handleUpdate}/>
                </div>);
    }
});
module.exports = OutputComponent;

