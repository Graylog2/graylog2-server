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

export const CAROUSEL_CONTAINER_CLASS_NAME = 'carousel-container';

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
 * Carousel component based on embla carousel. It needs to be wrapped with the CarouselProvider.
 * The CarouselProvider allows accessing the carouselApi object in all children, like the carousel navigation components.
 * The CarouselProvider also maintains the carousel configuration options.
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

    .${CAROUSEL_CONTAINER_CLASS_NAME} {
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
      <div className={CAROUSEL_CONTAINER_CLASS_NAME} ref={containerRef}>
        {children}
      </div>
    </StyledDiv>
  );
};

Carousel.Slide = CarouselSlide;
export default Carousel;
