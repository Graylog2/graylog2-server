// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

/**
 * Component to display horisontal space.
 * Mainly needed for responsive bootstrap grids.
 * Can be used in combination with bootstrap Cols to show horizontal space only for specific viewports.
 */

type Props = {
  /** Needed height in px */
  height: number,
  /** Allow custom classNames */
  className?: string,
};

const Spacer: StyledComponent<Props, {}, HTMLDivElement> = styled.div`
  width: 100%;
  height: ${(props) => props.height}px;
`;

const HorizontalSpacer = ({ height, className }: Props) => <Spacer height={height} className={className} />;

HorizontalSpacer.propTypes = {
  height: PropTypes.number,
  className: PropTypes.string,
};

HorizontalSpacer.defaultProps = {
  height: 10,
  className: undefined,
};

export default HorizontalSpacer;
