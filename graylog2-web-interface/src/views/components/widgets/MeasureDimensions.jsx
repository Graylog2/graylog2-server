// @flow strict
import * as React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

const MeasureDimensions = createReactClass({
  propTypes: {
    children: PropTypes.node.isRequired,
  },

  getInitialState() {
    return {
      height: undefined,
    };
  },

  componentDidMount() {
    window.addEventListener('resize', this._setHeight);
    this._setHeight();
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._setHeight);
  },

  _setHeight() {
    this.setState({ height: this._getHeight() });
  },

  _getHeight() {
    if (this.container) {
      return this.container.offsetHeight;
    }

    return undefined;
  },

  _renderChildren() {
    return React.Children.map(this.props.children, (child) => {
      return React.cloneElement(child, {
        containerHeight: this.state.height,
      });
    });
  },

  render() {
    return (
      <span ref={(node) => { this.container = node; }} style={{ display: 'block', height: '100%' }}>
        {this._renderChildren()}
      </span>
    );
  },
});

export default MeasureDimensions;
