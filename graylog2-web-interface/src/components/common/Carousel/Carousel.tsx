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
import React, { useContext } from 'react';
import styled from 'styled-components';

import CarouselSlide from './CarouselSlide';
import CarouselContext from './CarouselContext';

const useCarouselRef = (carouselId: string) => {
  const carouselContext = useContext(CarouselContext);

  if (!carouselContext) {
    throw new Error('Carousel component needs to be used inside CarouselProvider.');
  }

  if (!carouselContext[carouselId]) {
    throw new Error(`CarouselContext does not contain anything for carousel id ${carouselId}`);
  }

  return carouselContext[carouselId].ref;
};

/*
 * Carousel component based on embla carousel. Needs to be wrapped in CarouselProvider.
 * The CarouselProvider also allows configuring the carousel.
 */

type Props = {
  children: React.ReactNode,
  className?: string
  containerRef?: React.Ref<HTMLDivElement>
  carouselId: string
};

const StyledDiv = styled.div`
  &.carousel {
    overflow: hidden;

    .carousel-container {
      backface-visibility: hidden;
      display: flex;
      flex-direction: row;
      height: auto;
    }
  }
`;

const Carousel = ({ children, className, containerRef, carouselId }: Props) => {
  const carouselRef = useCarouselRef(carouselId);

  return (
    <StyledDiv className={`carousel ${className}`} ref={carouselRef}>
      <div className="carousel-container" ref={containerRef}>
        {children}
      </div>
    </StyledDiv>
  );
};

Carousel.defaultProps = {
  className: undefined,
  containerRef: undefined,
};

Carousel.Slide = CarouselSlide;
export default Carousel;
