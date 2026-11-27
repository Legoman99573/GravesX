<div align="center">
	<img width="80" height="80" src="/images/graves_icon_right.png">
	<img src="/images/graves_logo.png">
	<img width="80" height="80" src="/images/graves_icon_left.png">
	<br>
    <p>The <b>ULTIMATE</b> full-featured lightweight death chest plugin / player grave plugin! Every feature you could ever need and more! While still being lightweight and efficient.</p>
    <br>
	<img src="/images/screenshot_1.png">
	<img src="/images/screenshot_2.png">
	<br><br>
</div>

<b>Links:</b>
<ul>
	<li><b>Spigot:</b> https://www.spigotmc.org/resources/graves.74208/</li>
	<li><b>GitLab:</b> https://gitlab.com/ranull/minecraft/graves/</li>
</ul>

## Project layout

```
pom.xml    root parent: defines groupId + version for everything below (server and proxy always match)
lib/       global libs shared by server and proxy (Libby), installed by build-libs
common/    code shared by server and proxy (MiniMessage translator), shaded into both
server/    GravesX plugin (own pom.xml; server-only libs in server/libs)
proxy/     Velocity + BungeeCord plugins (own pom.xml; modules: core, velocity, bungeecord)
dist/      merges server + proxy into ONE jar for servers and proxies (own pom.xml)
Jenkinsfile
```

* Run `build-libs.sh` (or `build-libs.bat` on Windows) once to install the jars in `lib/` and `server/libs`, then `mvn clean package` from the root builds server, proxy and dist.
* Build one part from the root: `mvn -pl server -am clean package`, `mvn -pl proxy -am clean package` (`-am` also builds `common`). Building a module with `-f` on its own only works after `common` has been installed.
* Developer jars are separate per side and are copied into `dist/target` unmerged: server `GravesX-<version>-api.jar` / `-api-sources.jar` / `-api-javadoc.jar` (built in `server/`), proxy `GravesX-Proxy-<version>-api.jar` (includes relocated Libby) / `-api-sources.jar` / `-api-javadoc.jar` (built in `proxy/core/`).
* Change the version in the root `pom.xml` (`build.revision`); install `dist/target/GravesX-<version>.jar` on every backend server and the proxy.
* Proxy docs (global database, MiniMessage, server exclusion): `proxy/README.md`.
