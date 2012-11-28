/*
 *
 * Copyright (c) 2006-2010 Sam Collett (http://www.texotela.co.uk)
 * Dual licensed under the MIT (http://www.opensource.org/licenses/mit-license.php)
 * and GPL (http://www.opensource.org/licenses/gpl-license.php) licenses.
 *
 * Version 1.2
 * Demo: http://www.texotela.co.uk/code/jquery/numeric/
 *
 */
(function(e){e.fn.numeric=function(t,n){return t=t===!1?"":t||".",n=typeof n=="function"?n:function(){},this.data("numeric.decimal",t).data("numeric.callback",n).keypress(e.fn.numeric.keypress).blur(e.fn.numeric.blur)},e.fn.numeric.keypress=function(t){var n=e.data(this,"numeric.decimal"),r=t.charCode?t.charCode:t.keyCode?t.keyCode:0;if(r==13&&this.nodeName.toLowerCase()=="input")return!0;if(r==13)return!1;var i=!1;if(t.ctrlKey&&r==97||t.ctrlKey&&r==65)return!0;if(t.ctrlKey&&r==120||t.ctrlKey&&r==88)return!0;if(t.ctrlKey&&r==99||t.ctrlKey&&r==67)return!0;if(t.ctrlKey&&r==122||t.ctrlKey&&r==90)return!0;if(t.ctrlKey&&r==118||t.ctrlKey&&r==86||t.shiftKey&&r==45)return!0;if(r<48||r>57){if(r==45&&this.value.length==0)return!0;n&&r==n.charCodeAt(0)&&this.value.indexOf(n)!=-1&&(i=!1),r!=8&&r!=9&&r!=13&&r!=35&&r!=36&&r!=37&&r!=39&&r!=46?i=!1:typeof t.charCode!="undefined"&&(t.keyCode==t.which&&t.which!=0?(i=!0,t.which==46&&(i=!1)):t.keyCode!=0&&t.charCode==0&&t.which==0&&(i=!0)),n&&r==n.charCodeAt(0)&&(this.value.indexOf(n)==-1?i=!0:i=!1)}else i=!0;return i},e.fn.numeric.blur=function(){var t=e.data(this,"numeric.decimal"),n=e.data(this,"numeric.callback"),r=e(this).val();if(r!=""){var i=new RegExp("^\\d+$|\\d*"+t+"\\d+");i.exec(r)||n.apply(this)}},e.fn.removeNumeric=function(){return this.data("numeric.decimal",null).data("numeric.callback",null).unbind("keypress",e.fn.numeric.keypress).unbind("blur",e.fn.numeric.blur)}})(jQuery);