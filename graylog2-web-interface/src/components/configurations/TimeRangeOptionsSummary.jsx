import React from 'react';

const TimeRangeOptionsSummary = React.createClass({
  propTypes: {
    options: React.PropTypes.object.isRequired,
  },

  render() {
    let timerangeOptionsSummary = null;
    if (this.props.options) {
      timerangeOptionsSummary = Object.keys(this.props.options).map((key, idx) => {
        return (
          <span key={`timerange-options-summary-${idx}`}>
            <dt>{key}</dt>
            <dd>{this.props.options[key]}</dd>
          </span>
        );
      });
    }

    return (
      <dl className="deflist">
        {timerangeOptionsSummary}
      </dl>
    );
  },
});

export default TimeRangeOptionsSummary;
