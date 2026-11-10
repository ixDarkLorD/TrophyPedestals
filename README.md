# Trophy Pedestals website

Pedestals to show off what you've done in Minecraft: display pedestals for any item, mob trophies that keep the mob's
look, player statues, five glowing ranks, and trophy cabinets and wall shelves in every wood.

This `docs` branch of [ixDarkLorD/TrophyPedestals](https://github.com/ixDarkLorD/TrophyPedestals) holds the Trophy
Pedestals website: its homepage, guide and reference, at **https://ixdarklord.github.io/TrophyPedestals/**. The mod's
code is on the version branches (`26.1.2` for now), and bugs and ideas go to
[Issues](https://github.com/ixDarkLorD/TrophyPedestals/issues).

The site is built with [Zensical](https://zensical.org): `pip install zensical`, then `zensical serve` to preview and
`zensical build` to build. Pushing to this branch builds it and publishes it through GitHub Pages.

The reference and the changelog are generated from the mod's own docs, so they never drift from it: edit
`docs/reference.md` or `CHANGELOG.md` on the `26.1.2` branch, then run `python tools/sync_docs.py <path to that
checkout>` here (it defaults to `../TrophyPedestals-26.1.2`). The homepage (`overrides/home.html`), the overview,
guide, downloads and license pages, and the look (`docs/assets/pedestals.css` and `pedestals.js`) are the site's own.
