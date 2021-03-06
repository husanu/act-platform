package no.mnemonic.act.platform.auth.properties;

import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.properties.model.FunctionIdentifier;
import no.mnemonic.act.platform.auth.properties.model.OrganizationIdentifier;
import no.mnemonic.act.platform.auth.properties.model.SubjectIdentifier;
import no.mnemonic.services.common.auth.model.FunctionIdentity;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SubjectIdentity;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PropertiesBasedIdentityResolverTest {

  private final IdentityResolver resolver = new PropertiesBasedIdentityResolver();

  @Test
  public void testResolveFunctionIdentity() {
    String name = "test";
    FunctionIdentity identity = resolver.resolveFunctionIdentity(name);
    assertTrue(identity instanceof FunctionIdentifier);
    assertEquals(name, FunctionIdentifier.class.cast(identity).getName());
  }

  @Test
  public void testResolveOrganizationIdentity() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    OrganizationIdentity identity = resolver.resolveOrganizationIdentity(id);
    assertTrue(identity instanceof OrganizationIdentifier);
    assertEquals(id, OrganizationIdentifier.class.cast(identity).getGlobalID());
  }

  @Test
  public void testResolveSubjectIdentity() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    SubjectIdentity identity = resolver.resolveSubjectIdentity(id);
    assertTrue(identity instanceof SubjectIdentifier);
    assertEquals(id, SubjectIdentifier.class.cast(identity).getGlobalID());
  }

}
