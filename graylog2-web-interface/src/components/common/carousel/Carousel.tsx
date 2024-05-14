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

import CarouselSlide from 'components/common/carousel/CarouselSlide';
import CarouselApiContext from 'components/common/carousel/CarouselContext';

const useCarouselRef = (carouselId: string) => {
  const carouselApiContext = useContext(CarouselApiContext);

  if (!carouselApiContext) {
    throw new Error('Carousel component needs to be used inside CarouselProvider.');
  }

  if (!carouselApiContext[carouselId]) {
    throw new Error(`CarouselApiContext does not contain anything for carousel id ${carouselId}`);
  }

  return carouselApiContext[carouselId].ref;
};

type Props = {
  children: React.ReactNode,
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

const Carousel = ({ children, carouselId }: Props) => {
  const carouselRef = useCarouselRef(carouselId);

  return (
    <StyledDiv className="carousel" ref={carouselRef}>
      <div className="carousel-container">
        {children}
      </div>
    </StyledDiv>
  );
};

Carousel.Slide = CarouselSlide;
export default Carousel;
