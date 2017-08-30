package no.mnemonic.act.platform.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.api.service.v1.TraversalResult;
import no.mnemonic.act.platform.rest.api.AbstractEndpoint;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.UUID;

@Path("/v1/object")
@Api(tags = {"experimental"})
public class ObjectEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public ObjectEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve an Object by its UUID.",
          notes = "This operation returns an Object identified by its UUID. The result includes statistics about the " +
                  "Facts linked to the requested Object. The request will be rejected with a 403 if a user does not have " +
                  "access to any Facts linked to the requested Object.",
          response = Object.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getObjectById(
          @PathParam("id") @ApiParam(value = "UUID of the requested Object.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.getObject(getHeader(), new GetObjectByIdRequest().setId(id)));
  }

  @GET
  @Path("/{type}/{value}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve an Object by its type and value.",
          notes = "This operation returns an Object identified by its ObjectType and its value. The result includes " +
                  "statistics about the Facts linked to the requested Object. The requested type name needs to be " +
                  "globally unique, otherwise the Object needs to be fetched by it's UUID. The request will be rejected " +
                  "with a 403 if a user does not have access to any Facts linked to the requested Object.",
          response = Object.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getObjectByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of the requested Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of the requested Object.") @NotBlank String value
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.getObject(getHeader(), new GetObjectByTypeValueRequest().setType(type).setValue(value)));
  }

  @POST
  @Path("/uuid/{id}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve Facts bound to a specific Object.",
          notes = "This operation returns the Facts linked to a specific Object which is identified by its UUID. " +
                  "With the request body the user can specify which Facts will be included in the result. Only the " +
                  "Facts a user has access to will be returned. The request will be rejected with a 403 if a user " +
                  "does not have access to any Facts linked to the requested Object.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjectFactsById(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectFacts(getHeader(), request.setObjectID(id)));
  }

  @POST
  @Path("/{type}/{value}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve Facts bound to a specific Object.",
          notes = "This operation returns the Facts linked to a specific Object which is identified by its ObjectType " +
                  "and its value. With the request body the user can specify which Facts will be included in the result. " +
                  "Only the Facts a user has access to will be returned. The request will be rejected with a 403 if " +
                  "a user does not have access to any Facts linked to the requested Object.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjectFactsByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotBlank String value,
          @ApiParam(value = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectFacts(getHeader(), request.setObjectType(type).setObjectValue(value)));
  }

  @POST
  @Path("/uuid/{id}/traverse")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at an Object identified by its UUID.",
          notes = "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
                  "Objects are represented as graph vertices and Facts as graph edges. If a Fact binds together multiple " +
                  "Objects the graph will contain an edge between each Object pair (considering the binding directions). " +
                  "The labels of vertices and edges are the names of the corresponding ObjectTypes and FactTypes, " +
                  "respectively.\n\n" +
                  "The traversal query contained in the request must be a valid Gremlin query. Inside the query the graph " +
                  "is referenced as 'g' and the starting point of the traversal is set to the Object specified in the " +
                  "request. Therefore, it is not necessary to either instantiate a graph instance or to set the starting " +
                  "point of the traversal by using V() or E(). For example, a query to fetch all outgoing edges from the " +
                  "starting Object would simply be 'g.outE()'.\n\n" +
                  "If the result of the graph traversal are edges the response will contain the Facts belonging to the " +
                  "edges. If the result are vertices the response will contain Objects. In all other cases the result " +
                  "of the traversal is returned as-is, for instance, when the result is a list of vertex or edge properties.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response traverseObjectById(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseByObjectIdRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildTraversalResponse(service.traverseGraph(getHeader(), request.setId(id)));
  }

  @POST
  @Path("/{type}/{value}/traverse")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at an Object identified by its type and value.",
          notes = "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
                  "Objects are represented as graph vertices and Facts as graph edges. If a Fact binds together multiple " +
                  "Objects the graph will contain an edge between each Object pair (considering the binding directions). " +
                  "The labels of vertices and edges are the names of the corresponding ObjectTypes and FactTypes, " +
                  "respectively.\n\n" +
                  "The traversal query contained in the request must be a valid Gremlin query. Inside the query the graph " +
                  "is referenced as 'g' and the starting point of the traversal is set to the Object specified in the " +
                  "request. Therefore, it is not necessary to either instantiate a graph instance or to set the starting " +
                  "point of the traversal by using V() or E(). For example, a query to fetch all outgoing edges from the " +
                  "starting Object would simply be 'g.outE()'.\n\n" +
                  "If the result of the graph traversal are edges the response will contain the Facts belonging to the " +
                  "edges. If the result are vertices the response will contain Objects. In all other cases the result " +
                  "of the traversal is returned as-is, for instance, when the result is a list of vertex or edge properties.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response traverseObjectByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotBlank String value,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseByObjectTypeValueRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildTraversalResponse(service.traverseGraph(getHeader(), request.setType(type).setValue(value)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Search for Objects.",
          notes = "This operation searches for Objects and returns any matching Objects. The result includes statistics " +
                  "about the Facts linked to the returned Objects. With the request body the user can specify which " +
                  "Objects will be returned. Searching against linked Facts will only be performed one level deep, " +
                  "i.e. only Facts directly linked to an Object will be searched. The result will be restricted to the " +
                  "Objects and Facts a user has access to. In order to return an Object a user needs to have access to " +
                  "at least one Fact linked to the Object.",
          response = Object.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjects(
          @ApiParam(value = "Request to search for Objects.") @NotNull @Valid SearchObjectRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjects(getHeader(), request));
  }

  @POST
  @Path("/traverse")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph after performing an Object search.",
          notes = "This operation performs first an Object search and then traverses the graph of Objects and Facts " +
                  "starting at the Objects returned from the Object search. For more information about Object search " +
                  "see '/v1/object/search' and about graph traversal '/v1/object/{type}/{value}/traverse'. This operation " +
                  "accepts the same search parameters than '/v1/object/search' in addition to a Gremlin query for the " +
                  "graph traversal. Note that any limit provided in the request will only be applied to the Object search " +
                  "and not to the graph traversal. A limit to the graph traversal must be provided as part of the Gremlin query.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response traverseObjects(
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseByObjectSearchRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildTraversalResponse(service.traverseGraph(getHeader(), request));
  }

  private Response buildTraversalResponse(TraversalResult result) {
    // Add values returned from traversal to response.
    ResultStash.Builder resultStashBuilder = ResultStash.builder()
            .setSize(ObjectUtils.ifNotNull(result.getValues(), Collection::size, 0))
            .setData(result.getValues());
    // Also add any messages returned from traversal.
    result.getMessages().forEach(m -> resultStashBuilder.addActionError(m.getMessage(), m.getTemplate()));

    return resultStashBuilder.buildResponse();
  }

}
