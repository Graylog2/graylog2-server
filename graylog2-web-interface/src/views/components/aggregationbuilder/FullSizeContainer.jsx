// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: ({ height: number, width: number }) => React.Element<*>,
};
type State = {
  height: number,
  width: number,
};

class FullSizeContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      height: 0,
      width: 0,
    };
  }

  componentDidMount() {
    if (this.wrapper) {
      const height = this.wrapper.offsetHeight;
      const width = this.wrapper.offsetWidth;
      const { height: currentHeight, width: currentWidth } = this.state;
      if (height !== currentHeight || width !== currentWidth) {
        this.setState({ height, width });
      }
    }
  }

  componentDidUpdate() {
    if (this.wrapper) {
      const height = this.wrapper.offsetHeight;
      const width = this.wrapper.offsetWidth;
      const { height: currentHeight, width: currentWidth } = this.state;
      if (height !== currentHeight || width !== currentWidth) {
        // eslint-disable-next-line react/no-did-update-set-state
        this.setState({ height, width });
      }
    }
  }

  wrapper: ?HTMLDivElement;

  render() {
    const { children } = this.props;
    const { height, width } = this.state;
    return (
      <div ref={(elem) => { this.wrapper = elem; }} style={{ height: '100%', width: '100%' }}>
        {children({ height, width })}
      </div>
    );
  }
}

FullSizeContainer.propTypes = {
  children: PropTypes.func.isRequired,
};

export default FullSizeContainer;
