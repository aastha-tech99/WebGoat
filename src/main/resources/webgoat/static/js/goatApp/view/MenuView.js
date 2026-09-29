define(['jquery',
	'underscore',
	'backbone',
	'goatApp/model/MenuCollection',
	'goatApp/view/MenuItemView',
	'goatApp/support/GoatUtils'],
	function(
		$,
		_,
		Backbone,
		MenuCollection,
		MenuItemView,
		GoatUtils) {
	return  Backbone.View.extend({
		el:'#menu-container',
		//TODO: set template
		initialize: function() {
			this.collection = new MenuCollection();
			this.addSpinner();
			this.listenTo(this.collection,'menuData:loaded',this.render);
			// this.listenTo(this,'menu:click',this.accordionMenu);
			this.curLessonLinkId = '';
		},

		addSpinner: function() {
			//<i class="fa fa-spinner fa-spin"></i>
			this.$el.append($('<i>',{class:'fa fa-3x fa-spinner fa-spin'}));
		},

		removeSpinner: function() {
			this.$el.find('i.fa-spinner').remove();
		},

		// rendering top level menu
		render: function (){
			//for now, just brute force
			//TODO: refactor into sub-views/components
			this.removeSpinner();
			var items, catItems, stages;
			items = this.collection.models; // top level items
			var menuMarkup = '';
			var menuUl = $('<ul>',{class:'nano-content'});
			var self = this;
			items.forEach(function(item) { //CATEGORY LEVEL
				var catId, category, catLink, catArrow, catLinkText, lessonName, stageName;
				var translatedCatName = polyglot.t(item.get('name'));
				catId = GoatUtils.makeId(translatedCatName);
				category = $('<li>',{class:'sub-menu ng-scope'});
				catLink = $('<a>',{'category':catId});
				catArrow = $('<i>',{class:'fa fa-angle-right pull-right'});
				catLinkText = $('<span>',{text:translatedCatName});

				catLink.append(catArrow);
				catLink.append(catLinkText);
				catLink.click(_.bind(self.expandCategory,self,catId));
				category.append(catLink);
				// lesson level (first children level)
				var lessons = item.get('children');
				if (lessons) {
					var categoryLessonList = $('<ul>',{class:'slideDown lessonsAndStages',id:catId}); //keepOpen
					lessons.forEach(function(lesson) {
						var lessonItem = $('<li>',{class:'lesson'});
						var lessonName = polyglot.t(lesson.name);
						var lessonId = catId + '-' + GoatUtils.makeId(lessonName);
						if (self.curLessonLinkId === lessonId) {
							lessonItem.addClass('selected');
						}
						var lessonLink = $('<a>',{href:lesson.link,text:lessonName,id:lessonId});
						lessonLink.click(_.bind(self.onLessonClick,self,lessonId));
						lessonItem.append(lessonLink);
						//check for lab/stages
						categoryLessonList.append(lessonItem);
						if (lesson.complete) {
							lessonItem.append($('<span>',{class:'glyphicon glyphicon-check lesson-complete'}));
						}
						var stages = lesson.children;
						stages.forEach(function(stage, k) {
							var stageItem = $('<li>',{class:'stage'});
							var stageName = stage.name;
							var stageId = lessonId +  '-stage' + k;
							if (self.curLessonLinkId === stageId) {
								stageItem.addClass('selected');
							}
							var stageLink = $('<a>',{href:stage.link,text:stageName,id:stageId});
							stageLink.click(_.bind(self.onLessonClick,self,stageId));
							stageItem.append(stageLink);
							categoryLessonList.append(stageItem);
							if (stage.complete) {
								stageItem.append($('<span>',{class:'glyphicon glyphicon-check lesson-complete'}));
							}
						});
					});
					category.append(categoryLessonList);
				}

				menuUl.append(category);
			});
			this.$el.html(menuUl);
			//if we need to keep a menu open
			if (this.openMenu) {
				$('#'+this.openMenu).show();
			}
		},

		updateMenu: function() {
			//for now ...
			this.collection.fetch();
		},

		onLessonClick: function (elementId) {
			if (this.curLessonLinkId) {
				$('#'+this.curLessonLinkId).removeClass('selected').parent().removeClass('selected');
			}
			//update
			$('#'+elementId).addClass('selected').parent().addClass('selected');
			this.curLessonLinkId = elementId;
		},

		expandCategory: function (id) {
			if (id) {
			    //this.selectedCategory = id;
				this.accordionMenu(id);
			}
		},

		accordionMenu: function(id) {
	        if (this.openMenu !== id) {
	        	this.$el.find('#' + this.openMenu).slideUp(200);
	        	this.$el.find('#' + id).slideDown(300);
	        	this.openMenu = id;
	        } else { //it's open
	            this.$el.find('#' + id).slideUp(300).attr('isOpen', 0);
	            this.openMenu = null;
	            return;
	        }
		}
	});
});
