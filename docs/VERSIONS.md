# Versions and sources

Every version Trophy Pedestals builds against. It was split out of Tierwise on 2026-11-06 and pins the same toolchain
and libraries as Tierwise's 26.1.2 folder (`../Tierwise-26.1.2/docs/VERSIONS.md` has where each was checked).

| What | Version | Where |
|---|---|---|
| Minecraft | 26.1 – 26.1.2 (built on 26.1.2) | shared catalog `net.ixdarklord:sharedcatalogs:26.1.2-v2` |
| NeoForge | 26.1.2.112 (min 26.1.0.19-beta) | catalog |
| Fabric Loader / Fabric API | 0.19.3 / 0.155.3+26.1.2 (min API 0.145.1) | catalog |
| Gradle / Architectury Loom (no-remap) | 9.4.0 / 1.17.493 | wrapper; catalog |
| CoolCatLib | `8.2.0+26.1.2`, min 8.2.0 | `net.ixdarklord.coolcatlib:coolcatlib-<common|neoforge|fabric>`, from the ModResources Maven (branch `26.1.2`, d491eb8) |
| JEI (optional, compile-only) | 29.47.0.111 | `mezz.jei:jei-26.1.2-common-api` |
| KubeJS + Rhino (optional, NeoForge, compile-only) | 8.0.7 / 2101.2.8-build.91 | `maven.modrinth:kubejs:26.1.2-8.0.7+neoforge`, `maven.modrinth:rhino:2101.2.8-build.91+Rhino-1.21` |
| spark (dev runs, `-Ptest_spark=true`) | 1.10.187 | `maven.modrinth:spark` |
| MCP Bridge (dev runs only) | 1.0.0+26.1.2 | Maven Local |

Published to Maven Local as `net.ixdarklord.trophypedestals:trophypedestals-<common|neoforge|fabric>:0.1.0+26.1.2`, for Tierwise.
Minecraft APIs are the ones the pedestal code already used in Tierwise (checked with `javap` there); this split added
none.
