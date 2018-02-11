package de.mwg_bayreuth.mwgorganizer;


/**
 * The app uses android's "Shared Preferences" to store credentials, meta-data and settings
 * This class provides variables used inside the app, so the exact location where the prefs are
 * stored can be changed easily
 */
final class SharedPrefKeys {
    static String spPrefix = "de.mwg_bayreuth.mwgorganizer";

    // TODO: Shared Pref Keys for Version Control

    // Credentials
    static String credUsername = spPrefix + ".cred.username";
    static String credPassword = spPrefix + ".cred.password";

    // Vertretungsplan file information
    static String vplanPath              = spPrefix + ".vertplan.path";
    static String vplanLastUpdate        = spPrefix + ".vertplan.lastUpdate";
    static String vplanForceUpdate       = spPrefix + ".vertplan.forceUpdate";
    static String vplanButtonNr          = spPrefix + ".vertplan.buttons.number";
    static String vplanButtonLabel       = spPrefix + ".vertplan.buttons.label";
    static String vplanButtonShortLabel  = spPrefix + ".vertplan.buttons.shortlabel";
    static String vplanButtonFilename    = spPrefix + ".vertplan.buttons.filename";
    static String vplanButtonFilesize    = spPrefix + ".vertplan.buttons.filesize";
    static String vplanButtonFileUpdated = spPrefix + ".vertplan.buttons.fileupdated";

    // Mensa file information
    static String mensaLastUpdate        = spPrefix + ".mensa.lastUpdate";
    static String mensaForceUpdate       = spPrefix + ".mensa.forceUpdate";
    static String mensaButtonNr          = spPrefix + ".mensa.buttons.number";
    static String mensaButtonLabel       = spPrefix + ".mensa.buttons.label";
    static String mensaButtonShortLabel  = spPrefix + ".mensa.buttons.shorlabel";
    static String mensaButtonFilename    = spPrefix + ".mensa.buttons.filename";
    static String mensaButtonFilesize    = spPrefix + ".mensa.buttons.filesize";
    static String mensaButtonFileUpdated = spPrefix + ".mensa.buttons.fileupdated";

    // News
    static String newsLastUpdate  = spPrefix + ".news.lastUpdate";
    static String newsForceUpdate = spPrefix + ".news.forceUpdate";

    // TODO: Shared Pref Keys for Settings
    // TODO: Shared Pref Keys for Easter Eggs
}
