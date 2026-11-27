package dev.cwhead.GravesXCommon.libby;

/**
 * What to load. Use {@code {}} in package names where a dot belongs (for example {@code org{}h2}) so the shade
 * plugin never rewrites the coordinates or relocation patterns as class names.
 */
public final class LibraryRequest {
    final String groupId;
    final String artifactId;
    final String version;
    final String loaderId;
    final String relocateFrom;
    final String relocateTo;
    final boolean isolated;
    final boolean transitive;

    private LibraryRequest(Builder b) {
        this.groupId = b.groupId;
        this.artifactId = b.artifactId;
        this.version = b.version;
        this.loaderId = b.loaderId;
        this.relocateFrom = b.relocateFrom;
        this.relocateTo = b.relocateTo;
        this.isolated = b.isolated;
        this.transitive = b.transitive;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** {@code group:artifact:version} with {@code {}} turned into dots, for messages. */
    public String label() {
        return groupId.replace("{}", ".") + ":" + artifactId + ":" + version;
    }

    public static final class Builder {
        private String groupId;
        private String artifactId;
        private String version;
        private String loaderId;
        private String relocateFrom;
        private String relocateTo;
        private boolean isolated;
        private boolean transitive = true;

        private Builder() {}

        public Builder coordinates(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            return this;
        }

        /** Optional loader id for namespacing (recommended when isolated). */
        public Builder loaderId(String loaderId) {
            this.loaderId = loaderId;
            return this;
        }

        /** Relocates {@code from} to {@code to} at runtime. Both must be given together. */
        public Builder relocate(String from, String to) {
            this.relocateFrom = from;
            this.relocateTo = to;
            return this;
        }

        public Builder isolated(boolean isolated) {
            this.isolated = isolated;
            return this;
        }

        /** Whether to also resolve the library's own dependencies. Default true. */
        public Builder transitive(boolean transitive) {
            this.transitive = transitive;
            return this;
        }

        public LibraryRequest build() {
            if (groupId == null || groupId.isBlank()) {
                throw new IllegalArgumentException("groupID is required and cannot be blank.");
            }
            if (artifactId == null || artifactId.isBlank()) {
                throw new IllegalArgumentException("artifactID is required and cannot be blank.");
            }
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("version is required and cannot be blank.");
            }
            boolean hasFrom = relocateFrom != null && !relocateFrom.isBlank();
            boolean hasTo = relocateTo != null && !relocateTo.isBlank();
            if (hasFrom ^ hasTo) {
                throw new IllegalArgumentException("If relocation is used, both relocatePattern and relocateRelocatedPattern must be provided.");
            }
            return new LibraryRequest(this);
        }
    }
}
