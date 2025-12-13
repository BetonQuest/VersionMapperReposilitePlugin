package org.betonquest.reposilite.mapper.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.infrastructure.MavenRoutes;
import com.reposilite.shared.ContextDsl;
import com.reposilite.web.api.ReposiliteRoute;
import io.javalin.community.routing.Route;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import kotlin.Unit;
import org.betonquest.reposilite.mapper.integration.ArtifactsVersionsCache;
import org.betonquest.reposilite.mapper.integration.PomVersionedEntry;
import org.betonquest.reposilite.mapper.integration.VersionMapperFacade;
import org.betonquest.reposilite.mapper.settings.Artifact;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings({"MissingJavadoc", "PMD.CommentRequired", "PMD.ShortVariable"})
public class RestfulRoutes extends MavenRoutes implements RestfulDefinitions {

    private final Gson gson = new GsonBuilder().create();

    private final VersionMapperFacade baseFacade;

    @OpenApi(
            path = SERVICE_ACCESSOR_PATH,
            methods = HttpMethod.GET,
            tags = "Maven",
            summary = "Returns all versions with their downloadable jars by internal id.",
            description = "The internal id as defined in the reposilite configuration section.",
            pathParams = @OpenApiParam(name = "id", description = "The internal id of the artifact as defined in configuration.", required = true, example = "MyCoolArtifact"),
            queryParams = {
                    @OpenApiParam(name = SERVICE_ACCESSOR_QPARAM_NAME_SNAPSHOT, description = "Whether snapshot versions are listed." + SERVICE_ACCESSOR_QPARAM_DEFAULT_SNAPSHOT + " by default.", example = "false", type = Boolean.class),
                    @OpenApiParam(name = SERVICE_ACCESSOR_QPARAM_NAME_RELEASE, description = "Whether release versions are listed. " + SERVICE_ACCESSOR_QPARAM_DEFAULT_RELEASE + " by default.", example = "false", type = Boolean.class)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Valid result containing a list of all mapped versions with their jar paths", content = @OpenApiContent(from = String.class, type = ContentType.JSON)),
                    @OpenApiResponse(status = "204 ", description = "Valid result containing no entries"),
                    @OpenApiResponse(status = "404", description = "Internal id not found")
            }
    )
    private final ReposiliteRoute<Void> serviceAccess = new ReposiliteRoute<>(SERVICE_ACCESSOR_PATH_REPOSILITE, new Route[]{Route.HEAD, Route.GET}, context -> {
        serviceAccessHandler(context);
        return Unit.INSTANCE;
    });

    @OpenApi(
            path = SERVICE_DIRECT_PATH,
            methods = HttpMethod.GET,
            tags = "Maven",
            summary = "Returns all versions with their downloadable jars by their gav.",
            description = "Not using the accessor can cause delay if the versions have not been cached.",
            pathParams = {
                    @OpenApiParam(name = "repository", description = "Destination repository", required = true),
                    @OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
            },
            queryParams = {
                    @OpenApiParam(name = SERVICE_DIRECT_QPARAM_NAME_SNAPSHOT, description = "Whether snapshot versions are listed." + SERVICE_DIRECT_QPARAM_DEFAULT_SNAPSHOT + " by default.", example = "false", type = Boolean.class),
                    @OpenApiParam(name = SERVICE_DIRECT_QPARAM_NAME_RELEASE, description = "Whether release versions are listed. " + SERVICE_DIRECT_QPARAM_DEFAULT_RELEASE + " by default.", example = "false", type = Boolean.class)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Valid result containing a list of all mapped versions with their jar paths", content = @OpenApiContent(from = String.class, type = ContentType.JSON)),
                    @OpenApiResponse(status = "204 ", description = "Valid result containing no entries"),
                    @OpenApiResponse(status = "404", description = "Target not found")
            }
    )
    private final ReposiliteRoute<Void> serviceDirect = new ReposiliteRoute<>(SERVICE_DIRECT_PATH_REPOSILITE, new Route[]{Route.HEAD, Route.GET}, context -> {
        serviceDirectHandler(context);
        return Unit.INSTANCE;
    });

    public RestfulRoutes(final MavenFacade mavenFacade, final VersionMapperFacade baseFacade) {
        super(mavenFacade);
        this.baseFacade = baseFacade;
    }

    private void serviceDirectHandler(final ContextDsl<Void> context) {
        context.accessed(token -> {
            requireGav(context, gav -> {
                final String repository = context.requireParameter("repository");
                final Context ctx = context.getCtx();
                final Artifact artifact = baseFacade.findArtifact(repository, gav);
                if (artifact == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    return Unit.INSTANCE;
                }
                ctx.redirect(SERVICE_ACCESSOR_PATH.replace("{id}", artifact.id()), HttpStatus.TEMPORARY_REDIRECT);
                return Unit.INSTANCE;
            });
            return null;
        });
    }

    private void serviceAccessHandler(final ContextDsl<Void> context) {
        context.accessed(token -> {
            final Context ctx = context.getCtx();
            final String id = context.requireParameter("id");
            final ArtifactsVersionsCache artifactsVersionsCache = baseFacade.getArtifactsVersionsCache();

            if (!artifactsVersionsCache.hasEntry(id)) {
                ctx.status(HttpStatus.NOT_FOUND);
                return null;
            }

            final List<PomVersionedEntry> versions = artifactsVersionsCache.getVersions(id);
            if (versions.isEmpty()) {
                ctx.status(HttpStatus.NO_CONTENT).result("No versions found.");
                return null;
            }

            final boolean considerSnapshots = readOptionalQuery(ctx, SERVICE_ACCESSOR_QPARAM_NAME_SNAPSHOT, Boolean.class, SERVICE_ACCESSOR_QPARAM_DEFAULT_SNAPSHOT);
            final boolean considerReleases = readOptionalQuery(ctx, SERVICE_ACCESSOR_QPARAM_NAME_RELEASE, Boolean.class, SERVICE_ACCESSOR_QPARAM_DEFAULT_RELEASE);

            final Predicate<PomVersionedEntry> queryParamFilter = version ->
                    considerSnapshots && version.isSnapshot() || considerReleases && !version.isSnapshot();

            final JsonObject result = resolve(versions, queryParamFilter);

            ctx.status(HttpStatus.OK).result(gson.toJson(result));
            return null;
        });
    }

    private <T> T readOptionalQuery(final Context ctx, final String param, final Class<T> result, final T defaultValue) {
        return ctx.queryParamAsClass(param, result).getOrDefault(defaultValue);
    }

    private JsonObject resolve(final List<PomVersionedEntry> versions, final Predicate<PomVersionedEntry> queryParamFilter) {
        final Map<String, JsonArray> groups = versions.stream()
                .filter(queryParamFilter)
                .map(PomVersionedEntry::group)
                .map(group -> Map.entry(group, new JsonArray()))
                .distinct().collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
        versions.stream()
                .filter(queryParamFilter)
                .forEach(version -> {
                    groups.get(version.group()).add(buildPomEntries(version));
                });
        final JsonObject entry = new JsonObject();
        groups.forEach(entry::add);
        return entry;
    }

    private JsonObject buildPomEntries(final PomVersionedEntry entry) {
        final JsonObject parent = new JsonObject();
        final JsonObject pomVersions = new JsonObject();
        entry.pom().forEach(pomVersions::addProperty);
        parent.addProperty("maven", entry.maven());
        parent.addProperty("jar", entry.jarLocation().toString());
        parent.add("entries", pomVersions);
        return parent;
    }

    @Override
    public @NotNull Set<ReposiliteRoute<?>> getRoutes() {
        return Set.of(serviceDirect, serviceAccess);
    }
}
