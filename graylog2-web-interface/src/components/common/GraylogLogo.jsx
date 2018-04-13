import React from 'react';
import PropTypes from 'prop-types';

import GraylogLogoStyle from './GraylogLogo.css';

class GraylogLogo extends React.Component {
  static propTypes = {
    size: PropTypes.string,
    text: PropTypes.string,
    animated: PropTypes.bool,
  };

  static defaultProps = {
    size: 'sm',
    text: '',
    animated: false,
  };

  sizeToFactor() {
    const mapping = {
      sm: 0.25,
      m: 0.5,
      l: 1,
      xl: 2,
    };
    return mapping[this.props.size];
  }

  render() {
    const animation = this.props.animated ?
      (<animate attributeName="opacity" values="0;1;0" dur="2s" repeatCount="indefinite" />) : null;
    const height = 400 * this.sizeToFactor();
    const width = 400 * this.sizeToFactor();

    return (
      <div className={GraylogLogoStyle.background} >
        <svg className={GraylogLogoStyle.logo} viewBox="0 0 406.83749 393.125" height={height} width={width} id="svg3355">
          <g transform="matrix(1.25,0,0,-1.25,0,393.125)" id="g3363">
            <g transform="scale(0.1,0.1)" id="g3365">
              <circle cx="1630" cy="1570" r="1090" fill="white">
                {animation}
              </circle>
              <path id="circle"
                    style={{ fill: '#cb1f2a', 'fill-opacity': 1, 'fill-rule': 'nonzero', stroke: 'none' }}
                    d="m 553.973,1569.63 c 0,-577.06 496.247,-1084.849 1073.307,-1084.849 577.07,0 1073.31,507.789 1073.31,1084.849 0,623.17 -496.24,1090.59 -1073.31,1090.59 -577.06,0 -1073.307,-467.42 -1073.307,-1090.59 z M 1627.28,3145 c 946.39,0 1627.37,-727.09 1627.37,-1575.37 C 3254.65,709.809 2608.31,0 1627.28,0 646.344,0 0,709.809 0,1569.63 0,2417.91 680.898,3145 1627.28,3145" >
                {animation}
              </path>
              <path id="pulse"
                    style={{ fill: '#8e9093', 'fill-opacity': 1, 'fill-rule': 'nonzero', stroke: 'none' }}
                    d="m 942.496,1656.56 c 43.844,0 82.104,-23.96 102.374,-59.5 l 96.16,0 149.29,362.62 c 6.81,17.69 22.06,31.78 41.93,36.25 31.66,7.11 63.07,-12.77 70.18,-44.39 l 158.69,-700.82 161.77,945.16 0.05,0 c 3.82,22.36 20.47,41.41 43.9,47.18 31.49,7.73 63.33,-11.52 71.08,-43.04 l 174.91,-710.2 88.44,264.39 c 3.52,12.61 11.25,24.14 22.8,32.27 26.55,18.67 63.22,12.31 81.89,-14.26 l 76.33,-108.09 c 2.4,-25.55 3.93,-51.36 3.93,-77.53 0,-34.65 -2.85,-68.59 -7.03,-102.13 -11.12,4.19 -21.17,10.39 -27.9,19.95 l -0.09,-0.08 -74.43,105.4 -116.87,-349.34 c -10.28,-30.76 -43.55,-47.34 -74.31,-37.05 -19.78,6.64 -33.7,22.78 -38.37,41.65 l -0.05,0 -155.9,633.09 -164.1,-958.758 c -5.48,-32.023 -35.87,-53.551 -67.87,-48.082 -25.42,4.32 -44.22,24.371 -48.18,48.48 l -182.29,805.08 -94.25,-228.97 c -9.32,-22.67 -31.22,-36.38 -54.3,-36.38 l 0,-0.2 -136.1,0 c -20.45,-34.87 -58.344,-58.28 -101.684,-58.28 -65.039,0 -117.789,52.73 -117.789,117.79 0,65.04 52.75,117.79 117.789,117.79">
                {animation}
              </path>
            </g>
          </g>
        </svg>
        <span className={GraylogLogoStyle.text}>{this.props.text}</span>
      </div>);
  }
}

export default GraylogLogo;
