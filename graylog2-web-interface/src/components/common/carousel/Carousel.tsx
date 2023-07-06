import React from 'react';
import styled from 'styled-components';
import useEmblaCarousel from 'embla-carousel-react';

import CarouselSlide from 'components/common/carousel/CarouselSlide';

type Props = {
  children: React.ReactNode,
};

const StyledDiv = styled.div`
  &.carousel {
    overflow: hidden;

    .carousel__container {
      backface-visibility: hidden;
      display: flex;
      flex-direction: row;
      height: auto;
    }
  }
`;

const Carousel = ({ children }: Props) => {
  const [emblaRef] = useEmblaCarousel({ containScroll: 'trimSnaps' });

  return (
    <StyledDiv className="carousel" ref={emblaRef}>
      <div className="carousel__container">
        {children}
      </div>
    </StyledDiv>
  );
};

Carousel.Slide = CarouselSlide;
export default Carousel;
