/* The landing page's motion: motes of dust drifting up through the spotlight, the trophy panel stepping through the
   ranks, the display cases' loops, the adjust screen's picture strip, the demo's file tabs, and sections rising into
   view. Set up again after every instant navigation; with reduced motion nothing moves. */
(function () {
  "use strict";

  var reduced = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var timers = [];
  var frame = 0;

  function clear() {
    timers.forEach(function (t) { clearTimeout(t); clearInterval(t); });
    timers = [];
    if (frame) cancelAnimationFrame(frame);
    frame = 0;
  }

  // Motes of dust over the gallery: small squares drifting slowly up, brightest in the middle, where the spotlight is.
  function motes(canvas) {
    var ctx = canvas.getContext("2d");
    var dots = [];
    function size() {
      var ratio = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = canvas.clientWidth * ratio;
      canvas.height = canvas.clientHeight * ratio;
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
      dots = [];
      var count = Math.round(canvas.clientWidth * canvas.clientHeight / 9000);
      for (var i = 0; i < count; i++) {
        dots.push({
          x: Math.random() * canvas.clientWidth,
          y: Math.random() * canvas.clientHeight,
          s: Math.random() < 0.25 ? 3 : 2,
          p: Math.random() * Math.PI * 2,
          v: 6 + Math.random() * 14
        });
      }
    }
    var last = 0;
    function draw(t) {
      var dt = last ? Math.min(0.05, (t - last) / 1000) : 0;
      last = t;
      var w = canvas.clientWidth, h = canvas.clientHeight;
      ctx.clearRect(0, 0, w, h);
      dots.forEach(function (d) {
        if (!reduced) {
          d.y -= d.v * dt;
          d.x += Math.sin(d.p + t / 2400) * 4 * dt;
          if (d.y < -4) { d.y = h + 4; d.x = Math.random() * w; }
        }
        // Brighter in the middle third, under the spotlight.
        var mid = 1 - Math.min(1, Math.abs(d.x / w - 0.5) * 2.4);
        var a = (0.12 + 0.6 * mid) * (0.6 + 0.4 * Math.sin(d.p + t / 900));
        ctx.fillStyle = "rgba(255, 226, 160," + Math.max(0, a).toFixed(3) + ")";
        ctx.fillRect(Math.round(d.x), Math.round(d.y), d.s, d.s);
      });
      if (!reduced) frame = requestAnimationFrame(draw);
    }
    size();
    window.addEventListener("resize", size, { passive: true });
    frame = requestAnimationFrame(draw);
  }

  // The trophy panel: one trophy of each rank in turn, as its text panel shows over a pedestal in game.
  var TROPHIES = [
    { rank: "common", name: "Common", what: "Zombie", who: "Slain by Steve" },
    { rank: "uncommon", name: "Uncommon", what: "Creeper", who: "Slain by Alex" },
    { rank: "rare", name: "Rare", what: "Iron Age", who: "Unlocked by Steve" },
    { rank: "epic", name: "Epic", what: "Warden", who: "Slain by Alex" },
    { rank: "legendary", name: "Legendary", what: "Ender Dragon", who: "Slain by Steve" }
  ];
  var COLORS = { common: "#b8b8c2", uncommon: "#7cc35a", rare: "#4a9dff", epic: "#c06bff", legendary: "#ffc24a" };
  function plate(root) {
    var frame = root.querySelector(".tp-frame");
    if (!frame) return;
    var dots = root.querySelectorAll(".tp-plate__dots i");
    var fields = {
      rank: frame.querySelector(".tp-frame__rank"), what: frame.querySelector(".tp-frame__what"),
      who: frame.querySelector(".tp-frame__who"), when: frame.querySelector(".tp-frame__when")
    };
    var today = new Date().toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
    var at = 0;
    function show(i) {
      var t = TROPHIES[i];
      frame.setAttribute("data-rank", t.rank);
      frame.style.setProperty("--c", COLORS[t.rank]);
      fields.rank.textContent = t.name;
      fields.what.textContent = t.what;
      fields.who.textContent = t.who;
      fields.when.textContent = today;
      dots.forEach(function (d, j) { d.classList.toggle("is-on", j === i); });
    }
    show(reduced ? TROPHIES.length - 1 : 0);
    if (reduced) return;
    timers.push(setInterval(function () { at = (at + 1) % TROPHIES.length; show(at); }, 2400));
  }

  // The loops in the display cases (the hero's and the rank wall's): each loads once it comes into view, then plays
  // while it's on screen (with reduced motion, their first frames stay still).
  function loops(root) {
    var videos = root.querySelectorAll("video[data-src]");
    var start = function (video, on) {
      if (on && !video.src) { video.preload = "auto"; video.src = video.dataset.src; video.load(); }
      if (reduced) return;
      if (on) video.play().catch(function () {}); else if (video.src) video.pause();
    };
    if (!("IntersectionObserver" in window)) { videos.forEach(function (v) { start(v, true); }); return; }
    var seen = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) { start(entry.target, entry.isIntersecting); });
    }, { rootMargin: "200px 0px" });
    videos.forEach(function (v) { seen.observe(v); });
  }

  function demo(root) {
    root.querySelectorAll("[data-demo]").forEach(function (button) {
      button.addEventListener("click", function () {
        root.querySelectorAll("[data-demo]").forEach(function (b) { b.classList.toggle("is-on", b === button); });
        root.querySelectorAll("[data-demo-panel]").forEach(function (p) { p.hidden = p.dataset.demoPanel !== button.dataset.demo; });
      });
    });
  }

  function rise(root) {
    var items = root.querySelectorAll("[data-rise]");
    if (!("IntersectionObserver" in window) || reduced) {
      items.forEach(function (el) { el.classList.add("is-in"); });
      return;
    }
    root.classList.add("tp-anim");
    // (Anything still waiting after a few seconds shows anyway.)
    timers.push(setTimeout(function () { items.forEach(function (el) { el.classList.add("is-in"); }); }, 4000));
    var seen = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        var el = entry.target;
        // Siblings rising together come in one after another.
        var index = Array.prototype.indexOf.call(el.parentNode.children, el);
        el.style.transitionDelay = Math.min(index, 6) * 60 + "ms";
        el.classList.add("is-in");
        seen.unobserve(el);
      });
    }, { rootMargin: "0px 0px -8% 0px" });
    items.forEach(function (el) { seen.observe(el); });
  }

  // The docs' ladder of pages: ticks the ones this reader has opened (remembered in this browser only).
  function ladderOfPages() {
    var read = [];
    try { read = JSON.parse(localStorage.getItem("trophypedestals-read") || "[]"); } catch (e) { read = []; }
    if (read.indexOf(location.pathname) < 0) read.push(location.pathname);
    try { localStorage.setItem("trophypedestals-read", JSON.stringify(read.slice(-200))); } catch (e) { /* (private window) */ }
    document.querySelectorAll(".md-sidebar--primary a.md-nav__link").forEach(function (link) {
      link.classList.toggle("is-read", read.indexOf(new URL(link.href, location.href).pathname) >= 0);
    });
  }

  // Showcase pictures open large on a click: the picture over a dimmed page, with its caption (or its description).
  // One listener on the document, so pages swapped in by instant navigation work too.
  var lightboxReady = false;
  function lightbox() {
    document.querySelectorAll(".tp .tp-shot img").forEach(function (img) {
      img.setAttribute("data-zoom", "");
      img.setAttribute("tabindex", "0");
      img.setAttribute("role", "button");
    });
    if (lightboxReady) return;
    lightboxReady = true;
    var open = null, opener = null;
    function close() {
      if (!open) return;
      var box = open;
      open = null;
      box.classList.remove("is-on");
      setTimeout(function () { box.remove(); }, 260);
      document.removeEventListener("keydown", onKey);
      if (opener) opener.focus();
    }
    function onKey(e) { if (e.key === "Escape") close(); }
    function show(img) {
      opener = img;
      var figure = img.closest("figure"), caption = figure && figure.querySelector("figcaption");
      var box = document.createElement("div");
      box.className = "tp-lightbox";
      box.setAttribute("role", "dialog");
      box.setAttribute("aria-modal", "true");
      box.setAttribute("aria-label", img.alt || "Picture");
      var big = document.createElement("img");
      big.src = img.currentSrc || img.src;
      big.alt = img.alt;
      var text = document.createElement("p");
      text.textContent = caption ? caption.textContent.trim() : img.alt;
      var button = document.createElement("button");
      button.type = "button";
      button.setAttribute("aria-label", "Close");
      button.textContent = "✕";
      box.append(button, big, text);
      box.addEventListener("click", close);
      document.body.appendChild(box);
      open = box;
      requestAnimationFrame(function () { box.classList.add("is-on"); });
      document.addEventListener("keydown", onKey);
      button.focus();
    }
    document.addEventListener("click", function (e) {
      var img = e.target.closest && e.target.closest(".tp .tp-shot img[data-zoom]");
      if (img) { e.preventDefault(); show(img); }
    });
    document.addEventListener("keydown", function (e) {
      if ((e.key === "Enter" || e.key === " ") && e.target.matches && e.target.matches(".tp .tp-shot img[data-zoom]")) { e.preventDefault(); show(e.target); }
    });
  }

  // The picture strip: a dot for each place it can stop (each picture that can lead the view), the one showing marked,
  // a click on one scrolling there. The dots are made again when the strip changes size (two showing or one).
  function gallery(root) {
    root.querySelectorAll(".tp-gallery").forEach(function (box) {
      var track = box.querySelector(".tp-gallery__track"), dots = box.querySelector(".tp-gallery__dots");
      var slides = track.children;
      if (!slides.length) return;
      function step() {
        return slides.length > 1 ? slides[1].offsetLeft - slides[0].offsetLeft : track.clientWidth;
      }
      function stops() {
        var showing = Math.max(1, Math.round(track.clientWidth / step()));
        return Math.max(1, slides.length - showing + 1);
      }
      function mark() {
        var at = Math.round(track.scrollLeft / step());
        Array.prototype.forEach.call(dots.children, function (dot, i) {
          dot.classList.toggle("is-on", i === at);
          dot.setAttribute("aria-current", i === at ? "true" : "false");
        });
      }
      function build() {
        dots.textContent = "";
        var n = stops();
        dots.hidden = n < 2;
        for (var i = 0; i < n; i++) {
          var dot = document.createElement("button");
          dot.type = "button";
          dot.className = "tp-gallery__dot";
          dot.setAttribute("aria-label", "Show picture " + (i + 1) + " of " + n);
          (function (i) {
            dot.addEventListener("click", function () { track.scrollTo({ left: i * step() }); });
          })(i);
          dots.appendChild(dot);
        }
        mark();
      }
      var pending = 0;
      track.addEventListener("scroll", function () {
        if (pending) return;
        pending = requestAnimationFrame(function () { pending = 0; mark(); });
      }, { passive: true });
      if (window.ResizeObserver) new ResizeObserver(build).observe(track);
      build();
    });
  }

  function setup() {
    clear();
    ladderOfPages();
    var root = document.querySelector(".tp");
    if (!root) return;
    var canvas = root.querySelector(".tp-stars");
    if (canvas) motes(canvas);
    plate(root);
    loops(root);
    gallery(root);
    lightbox();
    demo(root);
    rise(root);
  }

  if (window.document$ && window.document$.subscribe) window.document$.subscribe(setup);
  else document.addEventListener("DOMContentLoaded", setup);
})();
