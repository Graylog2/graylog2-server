/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

const Wrapper: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  height: 100%;
  width: 100%;
  overflow: hidden;
  grid-row: 2;
  grid-column: 1;
  -ms-grid-row: 2;
  -ms-grid-column: 1;
`;

type Dimensions = { height: number; width: number; };
type Props = {
  children: (dimentions: Dimensions) => React.ReactElement,
};
type State = {
  height: number,
  width: number,
};

class FullSizeContainer extends React.Component<Props, State> {
  private wrapper: HTMLDivElement | undefined | null;

  static propTypes = {
    children: PropTypes.func.isRequired,
  };

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

export default FullSizeContainer;
