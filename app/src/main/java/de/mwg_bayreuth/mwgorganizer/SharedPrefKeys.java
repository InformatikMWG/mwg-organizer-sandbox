package de.mwg_bayreuth.mwgorganizer;


/**
 * The app uses android's "Shared Preferences" to store credentials, meta-data and settings
 * This class provides variables used inside the app, so the exact location where the prefs are
 * stored can be changed easily
 */
final class SharedPrefKeys {
    static String spRoot = "de.mwg_bayreuth.mwgorganizer";

    // TODO: Shared Pref Keys for Version Control

    // Credentials
    static String credUsername = spRoot + ".cred.username";
    static String credPassword = spRoot + ".cred.password";

    // Vertretungsplan file information
    static String vplanPath              = spRoot + ".vertplan.path";
    static String vplanLastUpdate        = spRoot + ".vertplan.lastUpdate";
    static String vplanForceUpdate       = spRoot + ".vertplan.forceUpdate";
    static String vplanFileNr            = spRoot + ".vertplan.file.number";
    static String vplanFileLabel         = spRoot + ".vertplan.file.label";
    static String vplanFileShortLabel    = spRoot + ".vertplan.file.shortlabel";
    static String vplanFileFilename      = spRoot + ".vertplan.file.filename";
    static String vplanFileFilesize      = spRoot + ".vertplan.file.filesize";
    static String vplanFileUpdated       = spRoot + ".vertplan.file.updated";
    static String vplanFileDownloaded    = spRoot + ".vertplan.file.downloaded";

    // Mensa file information
    static String mensaLastUpdate        = spRoot + ".mensa.lastUpdate";
    static String mensaForceUpdate       = spRoot + ".mensa.forceUpdate";
    static String mensaFileNr            = spRoot + ".mensa.file.number";
    static String mensaFileLabel         = spRoot + ".mensa.file.label";
    static String mensaFileShortLabel    = spRoot + ".mensa.file.shorlabel";
    static String mensaFileFilename      = spRoot + ".mensa.file.filename";
    static String mensaFileFilesize      = spRoot + ".mensa.file.filesize";
    static String mensaFileUpdated       = spRoot + ".mensa.file.updated";
    static String mensaFileDownloaded    = spRoot + ".mensa.file.downloaded";

    // News
    static String newsLastUpdate  = spRoot + ".news.lastUpdate";
    static String newsForceUpdate = spRoot + ".news.forceUpdate";

    // TODO: Shared Pref Keys for Settings
    // TODO: Shared Pref Keys for Easter Eggs
}
