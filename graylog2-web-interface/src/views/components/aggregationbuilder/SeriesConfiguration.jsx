import React from 'react';
import PropTypes from 'prop-types';
import { ControlLabel, FormControl, FormGroup, HelpBlock } from 'react-bootstrap';

import { Button } from 'components/graylog';
import Series from 'views/logic/aggregationbuilder/Series';

export default class SeriesConfiguration extends React.Component {
  static propTypes = {
    series: PropTypes.instanceOf(Series).isRequired,
    onClose: PropTypes.func.isRequired,
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      series: props.series,
      name: props.series.effectiveName,
    };
  }

  _onSubmit = () => {
    const { series, name } = this.state;
    if (name && name !== series.effectiveName) {
      const { config } = series;
      const newConfig = config.toBuilder().name(name).build();
      const newSeries = series.toBuilder().config(newConfig).build();
      this.props.onClose(newSeries);
    } else {
      this.props.onClose(series);
    }
  };

  _changeName = e => this.setState({ name: e.target.value });

  render() {
    const { name } = this.state;
    return (
      <span>
        <FormGroup>
          <ControlLabel>Name</ControlLabel>
          <FormControl type="text" value={name} onChange={this._changeName} />
          <HelpBlock>The name of the series as it appears in the chart.</HelpBlock>
        </FormGroup>
        <div className="pull-right" style={{ marginBottom: '10px' }}>
          <Button bsStyle="success" onClick={this._onSubmit}>Done</Button>
        </div>
      </span>
    );
  }
}
