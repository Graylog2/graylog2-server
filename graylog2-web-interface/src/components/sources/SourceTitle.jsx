import React, { PropTypes } from 'react';

const SourceTitle = React.createClass({
  propTypes: {
    className: PropTypes.string,
    resetFilters: PropTypes.func.isRequired,
    resetFilterId: PropTypes.string,
    children: PropTypes.oneOfType([
      PropTypes.array,
      PropTypes.element,
      PropTypes.string,
    ]).isRequired,
  },
  render() {
    return (
      <h3 className="sources-title">
        {this.props.children}
        <span style={{ marginLeft: 10 }}>
          <button id={this.props.resetFilterId} className={`btn btn-info btn-xs ${this.props.className}`}
                    onClick={this.props.resetFilters} title="Reset filter" style={{ display: 'none' }}>
              Reset
            </button>
        </span>
      </h3>
    );
  },
});

export default SourceTitle;
