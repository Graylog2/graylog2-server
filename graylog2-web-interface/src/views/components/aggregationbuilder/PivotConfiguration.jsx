import React from 'react';
import PropTypes from 'prop-types';

import { Button } from 'components/graylog';
import FieldType from 'views/logic/fieldtypes/FieldType';

import TimeHistogramPivot from './pivottypes/TimeHistogramPivot';
import TermsPivotConfiguration from './pivottypes/TermsPivotConfiguration';

import CustomPropTypes from '../CustomPropTypes';

const _configurationComponentByType = (type, value, onChange) => {
  switch (type.type) {
    case 'date': return <TimeHistogramPivot onChange={onChange} value={value} />;
    default: return <TermsPivotConfiguration onChange={onChange} value={value} />;
  }
};

export default class PivotConfiguration extends React.Component {
  static propTypes = {
    type: CustomPropTypes.instanceOf(FieldType).isRequired,
    config: PropTypes.object.isRequired,
    onClose: PropTypes.func.isRequired,
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      config: props.config,
    };
  }

  // eslint-disable-next-line react/destructuring-assignment
  _onSubmit = () => this.props.onClose(this.state);

  _onChange = config => this.setState({ config });

  render() {
    const { type } = this.props;
    const { config } = this.state;
    const typeSpecificConfiguration = _configurationComponentByType(type, config, this._onChange);
    return (
      <span>
        {typeSpecificConfiguration}
        <div className="pull-right" style={{ marginBottom: '10px' }}>
          <Button bsStyle="success" onClick={this._onSubmit}>Done</Button>
        </div>
      </span>
    );
  }
}
