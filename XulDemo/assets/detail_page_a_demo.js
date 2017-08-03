(function() {
    PWM_component_changed = function() {
        var poster_imgs = this.findItemsByClass("PWM-poster-image");
        var width = this.getAttr("width");
        var height = this.getAttr("height");
        var image = this.getAttr("image");
        for( var idx in poster_imgs ) {
            var item = poster_imgs[idx];
            item.setAttr("width", width);
            item.setAttr("height", height);
            item.setAttr("img.0", image);
        }

        var layer_mask = this.findItemById("@layer-mask");
        layer_mask.setAttr("width", width);
        layer_mask.setAttr("height", height);

        layer_mask.parent.setAttr("y", parseInt(height) + 4);
    };

    STAR_component_changed = function() {
        var star_size = this.getAttr("star-size");
        var star_num = this.getAttr("star-num");
        if (!star_size) {
            star_size = 24;
        } else {
            star_size = parseInt(star_size);
        }
        if (!star_num) {
            star_num = 0;
        } else {
            star_num = parseFloat(star_num);
        }
        star_num = Math.max(Math.min(5,star_num),0);

        var img = this.children[0];

        if (this.__star_size != star_size) {
            this.__star_size = star_size;
            img.setAttr("width", 5*star_size);
            img.setAttr("height", star_size);
            for( var i=0; i<5; ++i ) {
                img.setAttr("img."+i+".width", star_size);
                img.setAttr("img."+i+".height", star_size);
                var padding_value = "0," + (i*star_size) + "," + ((4-i)*star_size)+",0";
                img.setAttr("img."+i+".padding", padding_value);
            }
        }

        if (this.__star_num != star_num) {
            this.__star_num = star_num;
            var FULL_STAR_IMG = "file:///.assets/star/star-a1.png";
            var HALF_STAR_IMG = "file:///.assets/star/star-a3.png";
            var NONE_STAR_IMG = "file:///.assets/star/star-a2.png";
            for(var i=0; i<5; ++i) {
                var delta = star_num - i;
                if ( delta >= 1.0) {
                    img.setAttr("img."+i, FULL_STAR_IMG);
                } else if ( delta <= 0) {
                    img.setAttr("img."+i, NONE_STAR_IMG);
                } else {
                    img.setAttr("img."+i, HALF_STAR_IMG);
                }
            }
        }
    };

    UCC_component_changed = function() {
        var avatar = this.getAttr("avatar");
        var title = this.getAttr("title");
        var comment = this.getAttr("comment");

        var children = this.children;
        var avatar_img = children[0];
        avatar_img.setAttr("img.0", avatar);
        avatar_img.setAttr("text", title);
        var comment_label = children[1];
        comment_label.setAttr("text", comment);
    };


    var syncFocusBox = function(ownerPage, targetObject) {
        var floatFocusItem = ownerPage.findItemById("@float-focus-item");

        var parentSlider = targetObject.findParentByType("slider");

        var xDelta = 0;
        var yDelta = 0;
        if (parentSlider) {
            var sliderDirection = parentSlider.getAttr("direction");
            if (sliderDirection == "vertical") {
                yDelta = parentSlider.scrollDelta;
            } else {
                xDelta = parentSlider.scrollDelta;
            }
        }

        floatFocusItem.setAttr("x", parseInt(targetObject.viewX)+xDelta);
        floatFocusItem.setAttr("y", parseInt(targetObject.viewY)+yDelta);
        floatFocusItem.setAttr("width", targetObject.viewWidth);
        floatFocusItem.setAttr("height", targetObject.viewHeight);
    };

    DETAIL_PAGE_func_button_clicked = function() {
        var action = this.getAttr("action");
        if (action.indexOf("switch-to-") == 0) {
            var targetPage = action.substring(10);
            targetPage = "detail-page-"+targetPage;

            logger.d( "switch to:", targetPage);


            var ownerPage = this.ownerPage;
            var detailPages = ownerPage.findItemById("detail-pages");
            var lastPage = detailPages.__last_page_id;

            if (detailPages == targetPage) {
                return;
            }

            var oldCheckedItems = this.findParentByType("slider").findItemsByClass("icon-btn-image-checked");
            if (oldCheckedItems) {
                for( var idx in oldCheckedItems) {
                    oldCheckedItems[idx].removeClass("icon-btn-image-checked");
                }
            }
            this.addClass("icon-btn-image-checked");

            logger.d( "last page:", lastPage);
            if (lastPage) {
                lastPage = detailPages.findItemById(lastPage);
                if (lastPage) {
                    logger.d( "last page:", lastPage);
                    lastPage.removeClass("show-detail-page");
                }
            }

            detailPages.__last_page_id = targetPage;
            logger.d( "target page:", targetPage);
            targetPage = detailPages.findItemById(targetPage);
            if (targetPage) {
                logger.d( "target page:", targetPage);
                targetPage.addClass("show-detail-page");
            }
        } else if (action.indexOf("do-") == 0) {
            var actionName = action.substring(3);
            logger.d( "do:", actionName);
        }
    };


    DETAIL_PAGE_func_button_focused = function() {
        syncFocusBox(this.ownerPage, this);
    };

    DETAIL_PAGE_comment_item_focused = function() {
        syncFocusBox(this.ownerPage, this);
    };

})("detail_page_a_demo");
