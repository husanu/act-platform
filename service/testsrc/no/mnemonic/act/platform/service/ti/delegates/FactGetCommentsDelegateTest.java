package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.GetFactCommentsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class FactGetCommentsDelegateTest extends AbstractDelegateTest {

  @Test(expected = ObjectNotFoundException.class)
  public void testGetFactCommentsFactNotExists() throws Exception {
    FactGetCommentsDelegate.create().handle(new GetFactCommentsRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactCommentsNoAccessToFact() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID());
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(any());

    FactGetCommentsDelegate.create().handle(request);
  }

  @Test
  public void testGetFactComments() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID());
    List<FactCommentEntity> entities = createComments(request.getFact());
    mockFetchingComments(request, entities);

    ResultSet<FactComment> result = FactGetCommentsDelegate.create().handle(request);

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), result.getValues().size());
    verify(getFactCommentConverter(), times(entities.size())).apply(argThat(entities::contains));
  }

  @Test
  public void testGetFactCommentsFilterByBefore() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID()).setBefore(150L);
    List<FactCommentEntity> entities = createComments(request.getFact());
    mockFetchingComments(request, entities);

    assertEquals(1, FactGetCommentsDelegate.create().handle(request).getCount());
    verify(getFactCommentConverter()).apply(entities.get(0));
    verifyNoMoreInteractions(getFactCommentConverter());
  }

  @Test
  public void testGetFactCommentsFilterByAfter() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID()).setAfter(250L);
    List<FactCommentEntity> entities = createComments(request.getFact());
    mockFetchingComments(request, entities);

    assertEquals(1, FactGetCommentsDelegate.create().handle(request).getCount());
    verify(getFactCommentConverter()).apply(entities.get(2));
    verifyNoMoreInteractions(getFactCommentConverter());
  }

  private FactCommentEntity createComment(UUID factID, long timestamp) {
    return new FactCommentEntity()
            .setId(UUID.randomUUID())
            .setFactID(factID)
            .setReplyToID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setComment("Hello World!")
            .setTimestamp(timestamp);
  }

  private List<FactCommentEntity> createComments(UUID factID) {
    return ListUtils.list(createComment(factID, 100), createComment(factID, 200), createComment(factID, 300));
  }

  private void mockFetchingComments(GetFactCommentsRequest request, List<FactCommentEntity> entities) {
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity().setId(request.getFact()));
    when(getFactManager().fetchFactComments(request.getFact())).thenReturn(entities);
  }

}
