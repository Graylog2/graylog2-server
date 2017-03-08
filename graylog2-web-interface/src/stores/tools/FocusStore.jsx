import Reflux from 'reflux';
import $ from 'jquery';

const FocusStore = Reflux.createStore({
  focus: true,
  init() {
    $(window).blur(() => { this.trigger({ focus: false }); this.focus = false; });
    $(window).focus(() => { this.trigger({ focus: true }); this.focus = true; });
  },
  getInitialState() {
    return { focus: this.focus };
  },
});

export default FocusStore;
