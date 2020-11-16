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
import $ from 'jquery';

const UIUtils = {
  NAVBAR_HEIGHT: 55,
  scrollToHint(element) {
    if (!this.isElementVisible(element)) {
      const $scrollHint = $('#scroll-to-hint');

      $scrollHint
        .fadeIn('fast')
        .delay(1500)
        .fadeOut('fast')
        .on('click', (event) => {
          event.preventDefault();
          const top = window.pageYOffset - this.NAVBAR_HEIGHT + element.getBoundingClientRect().top;

          $('html, body').animate({ scrollTop: top }, 'fast');
          $scrollHint.off('click');
        });
    }
  },
  isElementVisible(element) {
    const rect = element.getBoundingClientRect();

    return rect.top > 0 && rect.bottom > 0;
  },
};

export default UIUtils;
