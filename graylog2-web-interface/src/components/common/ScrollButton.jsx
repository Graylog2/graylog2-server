import PropTypes from 'prop-types';
import React from 'react';

import ScrollButtonStyle from './ScrollButton.css';

class ScrollButton extends React.Component {
  static propTypes = {
    delay: PropTypes.number,
    scrollSteps: PropTypes.number,
    possition: PropTypes.string,
  };

  static defaultProps = {
    delay: 5,
    scrollSteps: 50,
    possition: '',
  };

  constructor() {
    super();

    this.state = {
      intervalId: 0,
      hideButton: true,
    };
  }

  componentDidMount() {
    window.addEventListener('scroll', this.showButton);
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.showButton);
  }

  showButton = () => {
    this.setState({ hideButton: window.scrollY === 0 });
  };

  scrollStep = () => {
    if (window.pageYOffset === 0) {
      clearInterval(this.state.intervalId);
    }
    window.scroll(0, window.pageYOffset - this.props.scrollSteps);
  };

  scrollToTop = () => {
    const intervalId = setInterval(this.scrollStep.bind(this), this.props.delay);
    this.setState({ intervalId: intervalId });
  };

  render() {
    if (this.state.hideButton) {
      return (<span />);
    }

    return (
      <button title="Back to top" className={`${ScrollButtonStyle.scroll} ${this.props.possition}`} onClick={this.scrollToTop}>
        <span className={`${ScrollButtonStyle.arrowUp} fa fa-chevron-up`} />
      </button>
    );
  }
}

export default ScrollButton;
