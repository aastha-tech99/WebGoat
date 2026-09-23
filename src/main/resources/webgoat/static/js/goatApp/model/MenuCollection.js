define(['jquery',
        'underscore',
        'backbone',
        'goatApp/model/MenuModel'],
    function ($, _, Backbone, MenuModel) {

        return Backbone.Collection.extend({
            model: MenuModel,
            url: 'service/lessonmenu.mvc',
            _fetching: false,

            initialize: function () {
                var self = this;
                this.fetch();
                setInterval(function () {
                    this.fetch()
                }.bind(this), 5000);
            },

            onDataLoaded: function () {
                this.trigger('menuData:loaded');
            },

            fetch: function () {
                if (this._fetching) { return; }
                this._fetching = true;
                var self = this;
                Backbone.Collection.prototype.fetch.apply(this, arguments).then(
                    function (data) {
                        self._fetching = false;
                        this.models = data;
                        self.onDataLoaded();
                    }
                ).fail(function () {
                    self._fetching = false;
                });
            }
        });
    });
