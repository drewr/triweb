TRINITY = Object();
TRINITY.defaults = Object();
TRINITY.nav = Object();

TRINITY.nav.expire_hover = function () {
  if (TRINITY.nav.menu) {
    TRINITY.nav.menu.find("ul").fadeOut("fast");
  }
};

TRINITY.nav.on = function() {
  if (TRINITY.nav.menu) {
    TRINITY.nav.menu.find("ul").fadeOut("fast");
    TRINITY.nav.menu = null;
  }
  if (!$(this).hasClass("center")) {
    $(this).addClass("active");
  }
  $(this).find("ul").show();
};

TRINITY.nav.off = function() {
  TRINITY.nav.menu = $(this);
  TRINITY.nav.menu.removeClass("active");
  setTimeout("TRINITY.nav.expire_hover()", 50);
};

TRINITY.nav.set_up = function() {
  TRINITY.nav.menu = null;
  $("div.nav > ul > li").hover(TRINITY.nav.on, TRINITY.nav.off);
};

TRINITY.init = function() {
  TRINITY.nav.set_up();
};

$(document).ready(TRINITY.init);
