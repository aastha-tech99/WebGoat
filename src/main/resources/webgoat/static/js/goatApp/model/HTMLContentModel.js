define(['jquery',
	'underscore',
	'backbone'],
	function($,_,Backbone) {
	return Backbone.Model.extend({
		_loading: false,

		fetch: function (options) {
			options = options || {};
			return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
		},

		loadData: function() {
			if (this._loading) { return; }
			this._loading = true;
			var self=this;
			this.fetch().then(function(data) {
				self._loading = false;
				self.setContent(data);
			}).fail(function() {
				self._loading = false;
			});
		},

		setContent: function(content) {
			this.set('content',content);
			this.checkNullModel();
			this.trigger('loaded');
		}
	});
});
