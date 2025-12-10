package org.betonquest.reposilite.mapper.restful;

@SuppressWarnings({"MissingJavadoc", "PMD.CommentRequired", "PMD.ConstantsInInterface"})
public interface RestfulDefinitions {

    String ROOT = "/mapper/";

    // ------------------- Service: direct -------------------

    String SERVICE_DIRECT_PREFIXED = ROOT + "service/direct/versions/";

    String SERVICE_DIRECT_PATH = SERVICE_DIRECT_PREFIXED + "{repository}/{gav}";

    String SERVICE_DIRECT_PATH_REPOSILITE = SERVICE_DIRECT_PREFIXED + "{repository}/<gav>";

    String SERVICE_DIRECT_QPARAM_NAME_SNAPSHOT = "snapshots";

    boolean SERVICE_DIRECT_QPARAM_DEFAULT_SNAPSHOT = true;

    String SERVICE_DIRECT_QPARAM_NAME_RELEASE = "releases";

    boolean SERVICE_DIRECT_QPARAM_DEFAULT_RELEASE = true;

    // ------------------- Service: accessor -------------------

    String SERVICE_ACCESSOR_PREFIXED = ROOT + "service/versions/";

    String SERVICE_ACCESSOR_PATH = SERVICE_ACCESSOR_PREFIXED + "{id}";

    String SERVICE_ACCESSOR_PATH_REPOSILITE = SERVICE_ACCESSOR_PATH;

    String SERVICE_ACCESSOR_QPARAM_NAME_SNAPSHOT = "snapshots";

    boolean SERVICE_ACCESSOR_QPARAM_DEFAULT_SNAPSHOT = true;

    String SERVICE_ACCESSOR_QPARAM_NAME_RELEASE = "releases";

    boolean SERVICE_ACCESSOR_QPARAM_DEFAULT_RELEASE = true;
}
