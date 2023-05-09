package es.optocom.jovp.definitions;

/**
 * Paradigm
 *
 * @since 0.0.1
 */
public enum Paradigm {
    /** Clicker response for detection or YES ANSWER */
    CLICKER,
    /** Two-alternative forced choice, horizontal setup */
    M2AFC,
    /** Two-alternative forced choice, vertical setup */
    M2AFC_VERTICAL,
    /** Three-alternative forced choice, horizontal setup */
    M3AFC,
    /** Three-alternative forced choice */
    M3AFC_VERTICAL,
    /** Four-alternative forced choice, cross setup */
    M4AFC,
    /** Four-alternative forced choice, diagonal setup */
    M4AFC_DIAGONAL,
    /** Eight-alternative forced choice */
    M8AFC,
    /** Nine-alternative forced choice */
    M9AFC
}