import $ from 'jquery';
import Control from 'can-control';

import template from './webpage.stache';

export default Control.extend({

  "init": function(element, options) {
    var webpageParam = null;
    var params = options.job.attr('outputParams');
    if (params) {
      for (var i = 0; i < params.attr('length'); i++) {
        if ((params.attr(i).attr('type') || '').toLowerCase() === 'webpage') {
          webpageParam = params.attr(i);
          break;
        }
      }
    }
    $(element).html(template({
      job: options.job,
      webpageParam: webpageParam
    }));
  }

});
