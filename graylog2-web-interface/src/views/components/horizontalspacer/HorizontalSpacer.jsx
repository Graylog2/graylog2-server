// @flow strict
import * as React from 'react';
import type { ComponentType } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

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

const Spacer: ComponentType<Props> = styled.div`
  width: 100%;
  height: ${props => props.height}px;
`;

const HorizontalSpacer = ({ height, className }: Props) => {
  return <Spacer height={height} className={className} />;
};

HorizontalSpacer.propTypes = {
  height: PropTypes.number,
  className: PropTypes.string,
};

HorizontalSpacer.defaultProps = {
  height: 10,
  className: undefined,
};

export default HorizontalSpacer;
