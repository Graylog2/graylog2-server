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
