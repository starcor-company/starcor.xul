// 当Layer item被选中时添加变换效果类，取消时移除变换效果类
function handleLayerItem() {
    var layerText = this.getAttr("text");
    var layerClass = this.getData("layer_class");
    logger.d("Layer Item:", layerText + " " + layerClass);

    var layerItem = this.ownerPage.findItemById("layer_demo")

    // 以"layer_mask"开头为layer-mask，需要继续找对应的mask view
    if (layerText.indexOf("layer_mask") >= 0) {
        var layerItems = layerItem.findItemsByClass(layerText);
        layerItem = layerItems[0];
    }

    if (this.hasClass("layer_item_checked")) {
        layerItem.addClass(layerClass);
    } else {
        layerItem.removeClass(layerClass);
    }
}

// 动态切换component
function switchComponent() {
    var component = this.findParentById("component_switch");
    logger.d("Component area id: ", component.id);

    if (component.getAttr("component") == "component_static") {
        component.setAttr("component", "component_dynamic");
    } else {
        component.setAttr("component", "component_static");
    }
    logger.d("Component id: ", component.getAttr("component"));
}

var SCRIPT_DEMO_TAG = "ScriptDemo:";
function logViewObject() {
    log("View.getStyle(name) font-size=", this.getStyle("font-size"));
    this.setStyle("font-size",36);
    log("View.setStyle(name, style) font-size=", this.getStyle("font-size"));

    log("View.getAttr(name) text=", this.getAttr("text"));
    this.setAttr("text", this.getAttr("text") + " changed");
    log("View.setAttr(name, attr) text=", this.getAttr("text"));

    log("View.getAction(name) action=", this.getAction("bindingReady"));
    log("View.getData(name) data=", this.getData("bindingReady_data"));

    log("View.viewX =", this.viewX);
    log("View.viewY =", this.viewY);
    log("View.viewWidth =", this.viewWidth);
    log("View.viewHeight =", this.viewHeight);
    log("View.id =", this.id);
    log("View.type =", this.type);
    log("View.binding =", this.binding);
    log("View.parent =", this.parent);
    log("View.ownerPage =", this.ownerPage);
    log("View.isFocusable =", this.isFocusable);
    log("View.isVisible =", this.isVisible);
    log("View.isEnabled =", this.isEnabled);
    log("View.hasFocus =", this.hasFocus);
    log("View.isFocused =", this.isFocused);
    log("View.classList =", this.classList);
    log("View.isBindingSuccess =", this.isBindingSuccess);
    log("View.isBindingReady =", this.isBindingReady);

    log("View.findParentById(id) parent=", this.findParentById("label_parent_id").id);
    log("View.findParentByClass(class) parent=", this.findParentByClass("label_parent_class").classList);
    log("View.findParentByType(type) parent=", this.findParentByClass("grid").type);
    log("View.isChildOf(view) =", this.isChildOf(this.ownerPage.findItemById("label_parent_id")));

    this.killFocus();
    log("View.killFocus() isFocused=", this.isFocused);
    this.requestFocus();
    log("View.requestFocus() isFocused=", this.isFocused);

    var TEST_CLASS = "class_for_test";
    log("View.hasClass(clsName) clsName=class_for_test result=", this.hasClass(TEST_CLASS));
    this.addClass(TEST_CLASS)
    log("View.addClass(clsName) clsName=class_for_test result=", this.hasClass(TEST_CLASS));
    this.removeClass(TEST_CLASS)
    log("View.removeClass(clsName) clsName=class_for_test result=", this.hasClass(TEST_CLASS));

    // 需要在java代码中处理
    this.dispatchEvent("blur", "usr_cmd", "viewBlur");
    log("View.dispatchEvent(“event”, “type”, “command”) ", "blur", "usr_cmd", "viewBlur");
    this.fireEvent("blur");

    this.fireEvent("focus");
    log("View.fireEvent(“event”) event=", "focus");
    this.blinkClass("quiver-item");
    log("View.blinkClass(“className0”,…) clsName=", "quiver-item");
}

function logAreaObject() {
    log("Area.findItemById(id) itemId=item1 result=", this.findItemById("item1").id);
    var children = this.findItemsByClass("label_focusable")
    for (var i=0; i < children.length; i++) {
        log("Area.findItemsByClass(clsName) class=label_focusable item=", children[i].id);
    }
    for (var i=0; i < this.children.length; i++) {
        log("Area.children item=", this.children[i].id);
    }
    this.setDynamicFocus(this.children[0]);
    log("Area.setDynamicFocus(view) view=", this.children[0].id);
}

function logSliderObject() {
    log("Slider.scrollRange =", this.scrollRange);
    log("Slider.scrollPos =", this.scrollPos);
    this.scrollTo(this.scrollRange, true);
    log("Slider.scrollTo(pos, [animation]) scrollTo=", this.scrollPos);
    this.activateScrollBar();
    log("Slider.activeScrollBar()", "scrollbar active");
    this.makeChildVisible(this.children[2]);
    log("Slider.makeChildVisible(child, [animation])", "item[2] is visible");
    this.makeChildVisible(this.children[3], 0.5, true);
    log("Slider.makeChildVisible(child, align, [animation])", "item[3] is visible");
    this.makeChildVisible(this.children[3], 0.5, 0, true);
    log("Slider.makeChildVisible( child, align,alignPoint, [animation] )", "item[3] is visible");
    this.scrollByPage(2, true);
    log("Slider.scrollByPage( pages, [animation] )", "scroll to right 2 page");
}

function log() {
    var array = [SCRIPT_DEMO_TAG];
    for (var i=0; i < arguments.length; i++) {
        array.push(" " + arguments[i]);
    }
    logger.d.apply(logger, array);
}



