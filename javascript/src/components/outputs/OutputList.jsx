'use strict';

var React = require('react/addons');
var OutputsStore = require('../../stores/outputs/OutputsStore');
var StreamsStore = require('../../stores/streams/StreamsStore');

var OutputList = React.createClass({
    OUTPUT_DATA_REFRESH: 5*1000,

    getInitialState() {
        return {
            outputs: [],
            streamId: this.props.streamId,
            permissions: JSON.parse(this.props.permissions)
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        var callback = (outputs) => {
            if (this.isMounted()) {
                this.setState({
                    outputs: outputs
                });
            }
        };
        if (this.state.streamId) {
            OutputsStore.loadForStreamId(this.state.streamId, callback);
        } else {
            OutputsStore.load(callback);
        }
    },
    _isPermitted(permissions) {
        var result = permissions.every((p) => this.state.permissions[p]);
        return result;
    },
    _sortByTitle(output1, output2) {
        return output1.title.localeCompare(output2.title);
    },
    _formatConfiguration(configuration) {
        var items = [];

        for (var key in configuration) {
            if (configuration.hasOwnProperty(key)) {
                items.push({key: key, value: configuration[key]});
            }
        }

        var formattedItems = items.map((i) => {
            return (<li>{i.key}: {i.value}</li>);
        });

        return (
            <ul>
                {formattedItems}
            </ul>
        );
    },
    _formatOutput(output) {
        var deletionForm = (this.state.streamId && this._isPermitted(["STREAM_OUTPUTS_DELETE"]) ? (
            <button className="btn btn-warning btn-xs"
                    onClick={this.removeOutputFromStream.bind(null, output.id, this.state.streamId)} data-confirm="Really delete output {output.title} from stream {this.state.stream.title}?">
                <i className="fa fa-remove"></i> Delete from stream
            </button>
        ) : (<form></form>));

        var terminationUrl = (this.state.streamId ? jsRoutes.controllers.OutputsController.terminate(output.id, this.state.streamId).url : jsRoutes.controllers.OutputsController.terminate(output.id).url);
        var terminationForm = (this._isPermitted(["OUTPUTS_TERMINATE"]) ? (
            <button className="btn btn-danger btn-xs" onClick={this.removeOutputGlobally.bind(null, output.id)}
                    data-confirm={"Really delete output " + output.title + " globally? It will be removed from all streams in the system."}>
                <i className="fa fa-remove"></i> Delete globally
            </button>
        ) : (<div></div>));

        var contentPack = (output.content_pack ? (<span title="Created from content pack"><i className="fa fa-gift"></i></span>) : (<div></div>));
        var configuration = (output.configuration.length == 0 ? (<ul><li>-- no configuration --</li></ul>) : this._formatConfiguration(output.configuration));

        var creatorUserLink = (<a href={jsRoutes.controllers.UsersController.show(output.creator_user_id).url}><i className="fa fa-user"></i> {output.creator_user_id}</a>);

        return (
            <div className="row content node-row">
                <div className="col-md-12">
                    <span className="pull-right node-row-info">
                        <span className="text">Started by {creatorUserLink}</span>

                        <span className="text" title={moment(output.created_at).format()}>{moment(output.created_at).fromNow()}</span>
                        &nbsp;
                        {deletionForm}
                        {terminationForm}
                    </span>
                    <h3>
                        <i className="fa fa-ellipsis-vertical"></i> {output.title} ({output.type})
                        {contentPack}
                    </h3>
                    <div className="well well-small">
                        {configuration}
                    </div>
                </div>
            </div>
        );
    },
    removeOutputGlobally(outputId) {
        if (confirm("Do you really want to terminate this output?")) {
            OutputsStore.remove(outputId, (jqXHR, textStatus, errorThrown) => {
                this.loadData();
            });
        }
    },
    removeOutputFromStream(outputId, streamId) {
        if (confirm("Do you really want to remove this output from the stream?")) {
            StreamsStore.removeOutput(streamId, outputId, (jqXHR, textStatus, errorThrown) => {
                this.loadData();
            });
        }
    },
    render() {
        var outputList;
        if (this.state.outputs.length == 0) {
            outputList = (<div className="row content node-row"><div className="col-md-12"><div className="alert alert-info">There are no outputs.</div></div></div>);
        } else {
            var outputs = this.state.outputs.sort(this._sortByTitle).map(this._formatOutput);
            outputList = (
                <div>{outputs}</div>
            )
        }

        return outputList;
    }
});

module.exports = OutputList;
