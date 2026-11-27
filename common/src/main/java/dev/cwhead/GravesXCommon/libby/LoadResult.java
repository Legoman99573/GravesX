package dev.cwhead.GravesXCommon.libby;

/** Outcome of {@link LibraryLoader#load(LibraryRequest)}. */
public enum LoadResult {
    /** The library is on the class path (or isolated class loader). */
    LOADED,
    /** It could not be built, downloaded or added; the reason was sent to the {@link LibraryLog}. */
    FAILED,
    /** It loaded, but the post-load check of an isolated + relocated library failed. The caller should treat this as fatal. */
    VERIFICATION_FAILED
}
