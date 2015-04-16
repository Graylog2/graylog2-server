'use strict';

var React = require('react');

var DataTable = React.createClass({
    render() {
        return (
            <div className="row">
                <div className="col-md-12">
                    <div id={this.props.id} className="data-table">
                        <table className="table table-striped">
                            <thead>
                                {this.props.headers}
                            </thead>
                            <tbody>
                                {this.props.rows}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = DataTable;