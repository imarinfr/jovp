package es.optocom.jovp.engine.structures;

/**
 *
 * Paradigm
 *
 * @since 0.0.1
 */
public enum Paradigm {
    CLICKER, // Clicker response for detection or YES ANSWER
    M2AFC_HORIZONTAL, // Two-alternative forced choice, horizontal setup
    M2AFC_VERTICAL, // Two-alternative forced choice, vertical setup
    M3AFC_HORIZONTAL, // Three-alternative forced choice, horizontal setup
    M3AFC_VERTICAL, // Three-alternative forced choice
    M4AFC_CROSS, // Four-alternative forced choice, cross setup
    M4AFC_DIAGONAL, // Four-alternative forced choice, diagonal setup
    M8AFC, // Eight-alternative forced choice
    M9AFC // Nine-alternative forced choice
}