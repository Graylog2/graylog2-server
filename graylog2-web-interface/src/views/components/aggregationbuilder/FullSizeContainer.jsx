// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

const Wrapper: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  height: 100%;
  width: 100%;
  overflow: hidden;
  grid-row: 2;
  grid-column: 1;
  -ms-grid-row: 2;
  -ms-grid-column: 1;
`;

type Props = {
  children: ({ height: number, width: number }) => React.Element<*>,
};
type State = {
  height: number,
  width: number,
};

class FullSizeContainer extends React.Component<Props, State> {
  wrapper: ?HTMLDivElement;

  constructor(props: Props) {
    super(props);
    this.state = {
      height: 0,
      width: 0,
    };
  }

  componentDidMount() {
    this._updateDimensions();
  }

  componentDidUpdate() {
    this._updateDimensions();
  }

  _updateDimensions() {
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

  render() {
    const { children } = this.props;
    const { height, width } = this.state;
    return (
      <Wrapper ref={(elem) => { this.wrapper = elem; }}>
        {children({ height, width })}
      </Wrapper>
    );
  }
}

FullSizeContainer.propTypes = {
  children: PropTypes.func.isRequired,
};

export default FullSizeContainer;
