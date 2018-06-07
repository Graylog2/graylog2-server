import React from 'react';
import PropTypes from 'prop-types';

class FullSizeContainer extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      height: 0,
      width: 0,
    };
  }
  componentDidMount() {
    if (this.wrapper) {
      const height = this.wrapper.offsetHeight;
      const width = this.wrapper.offsetWidth;
      if (height !== this.state.height || width !== this.state.width) {
        this.setState({ height, width });
      }
    }
  }
  componentDidUpdate() {
    if (this.wrapper) {
      const height = this.wrapper.offsetHeight;
      const width = this.wrapper.offsetWidth;
      if (height !== this.state.height || width !== this.state.width) {
        this.setState({ height, width });
      }
    }
  }
  render() {
    const { children } = this.props;
    const { height, width } = this.state;
    return (
      <div ref={(elem) => { this.wrapper = elem; }} style={{ height: '100%', width: '100%' }}>
        {React.cloneElement(children, { height, width })}
      </div>
    );
  }
}

FullSizeContainer.propTypes = {
  children: PropTypes.element.isRequired,
};

export default FullSizeContainer;
