import React from 'react';
import style from '!style!css!components/configurations/ConfigurationStyles.css';

const TimeRangeOptionsSummary = React.createClass({
  propTypes: {
    options: React.PropTypes.object.isRequired,
  },

  render() {
    let timerangeOptionsSummary = null;
    if (this.props.options) {
      timerangeOptionsSummary = Object.keys(this.props.options).map((key, idx) => {
        return (
          <span key={'timerange-options-summary-' + idx}>
            <dt>{key}</dt>
            <dd>{this.props.options[key]}</dd>
          </span>
        );
      });
    }

    return (
      <dl className={style.deflist}>
        {timerangeOptionsSummary}
      </dl>
    );
  },
});

export default TimeRangeOptionsSummary;
