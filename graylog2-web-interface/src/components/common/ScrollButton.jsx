import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { util } from 'theme';
import Icon from './Icon';

const ScrollBtn = styled.button(({ theme }) => `
  opacity: 0.3;
  background-color: ${theme.color.variant.primary};
  width: 40px;
  height: 40px;
  position: fixed;
  bottom: 60px;
  right: 20px;
  border-radius: 5px;
  border: none;

  &:hover {
    opacity: 1;
  }

  &.middle {
    right: 35%;
  }
`);

const ArrowUpIcon = styled(Icon)(({ theme }) => `
  color: ${util.readableColor(theme.color.variant.primary)};
  position: absolute;
  top: 50%;
  left: 50%;
  margin-top: -9px;
  margin-left: -5px;
`);

class ScrollButton extends React.Component {
  static propTypes = {
    delay: PropTypes.number,
    scrollSteps: PropTypes.number,
    position: PropTypes.string,
  };

  static defaultProps = {
    delay: 5,
    scrollSteps: 50,
    position: '',
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
    const { intervalId } = this.state;
    const { scrollSteps } = this.props;

    if (window.pageYOffset === 0) {
      clearInterval(intervalId);
    }
    window.scroll(0, window.pageYOffset - scrollSteps);
  };

  scrollToTop = () => {
    const { delay } = this.props;
    const intervalId = setInterval(this.scrollStep.bind(this), delay);

    this.setState({ intervalId: intervalId });
  };

  render() {
    const { position } = this.props;
    const { hideButton } = this.state;

    if (hideButton) {
      return (<span />);
    }

    return (
      <ScrollBtn title="Back to top"
                 type="button"
                 className={position}
                 onClick={this.scrollToTop}>
        <ArrowUpIcon name="chevron-up" />
      </ScrollBtn>
    );
  }
}

export default ScrollButton;
