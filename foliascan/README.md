# FoliaScan

This folder contains **FoliaScan**, a third-party tool used to check GravesX for
[Folia](https://github.com/PaperMC/Folia) compatibility.

## Credit

FoliaScan was created by **SalyyS1** and is not part of GravesX.

- Source: https://github.com/SalyyS1/foliascan
- Releases: https://github.com/SalyyS1/foliascan/releases
- Author: https://github.com/SalyyS1

All credit for FoliaScan belongs to its author. GravesX is not affiliated with or endorsed by the
FoliaScan project. For license terms, see the LICENSE in the upstream repository.

## What it does

FoliaScan is a bytecode scanner. It reads a compiled plugin jar and reports where the plugin breaks
under Folia's regionised threading, such as legacy `BukkitScheduler` calls, synchronous teleports and
entity access from the wrong thread. Its rules are derived from Folia's own source, and it can produce
console, JSON and SARIF output for CI.

It does not run the plugin. A clean report means no enabled rule matched, not that the plugin is
proven safe.

## Version

The reports in the GravesX Folia work were produced with **FoliaScan**.

## Usage

Requires Java 21 or newer.

```
java -jar foliascan-*.jar GravesX-20*.4.9.*.jar
java -jar foliascan-*.jar GravesX-20*.4.9.*.jar --verbose
java -jar foliascan-*.jar GravesX-20*.4.9.*.jar --all
java -jar foliascan-*.jar --rules
```

- `--verbose` lists every site instead of truncating with "... N more".
- `--all` also reports bundled and relocated dependencies, which are skipped by default.
- `--rules` prints the rule catalogue.

Options can differ between versions. Check the upstream README for the version you are using.

## Updating

Download newer releases from the upstream releases page above rather than relying from this repository though we will try to keep this up to date.