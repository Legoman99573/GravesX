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

## How to build

1. Clone the repository and ensure you have Java and Maven installed.
2. Run `build-libs.sh` (or `build-libs.bat` on Windows) once to install the required JARs from `lib/` into the local Maven repository and `server/libs`.
3. From the repository root, run `mvn clean package` to build the server, proxy, and distribution modules.
4. To build individual components, use:
    - **Server:** `mvn -pl server -am clean package`
    - **Proxy:** `mvn -pl proxy -am clean package`

   The `-am` flag also builds the required `common` module. Building a module independently with `-f` requires `common` to have already been installed.

5. Once the build completes, the distribution JAR will be available at `dist/target/GravesX-<version>.jar`. Install this JAR on every backend server and the proxy.

### Developer artifacts

Developer JARs are built separately for each side and copied into `dist/target` without being merged:

- **Server** (`server/`):
    - `GravesX-<version>-api.jar`
    - `GravesX-<version>-api-sources.jar`
    - `GravesX-<version>-api-javadoc.jar`
- **Proxy** (`proxy/core/`):
    - `GravesX-Proxy-<version>-api.jar` (includes relocated Libby)
    - `GravesX-Proxy-<version>-api-sources.jar`
    - `GravesX-Proxy-<version>-api-javadoc.jar`

### Important

**Self-compiled builds are not officially supported.** Support is provided only for official project releases or development builds. Issues encountered with locally compiled or modified builds must be reproduced using an official release or development builds before they can be considered for support.