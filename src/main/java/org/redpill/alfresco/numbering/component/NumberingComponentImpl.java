package org.redpill.alfresco.numbering.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redpill.alfresco.numbering.decorator.Decorator;
import org.redpill.alfresco.numbering.storage.NumberingStorage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * This component handles numbering of objects
 *
 * @author Marcus Svensson - Redpill Linpro AB
 *
 */
public class NumberingComponentImpl implements NumberingComponent, InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(NumberingComponentImpl.class);

  protected Repository repositoryHelper;
  protected NodeService nodeService;
  protected DictionaryService dictionaryService;
  protected String id;

  protected long startValue = 1;
  protected List<String> bindTypes = new ArrayList<>();
  protected List<String> ignoreTypes = new ArrayList<>();
  protected List<String> ignoreAspects = new ArrayList<>();
  protected List<QName> bindTypeQNames = new ArrayList<>();
  protected List<QName> ignoreTypeQNames = new ArrayList<>();
  protected List<QName> ignoreAspectQNames = new ArrayList<>();
  protected Decorator decorator;
  protected NamespaceService namespaceService;

  protected static final String MSG_ERROR_NOT_ALLOWED = "Get next number is not allowed";

  protected NumberingStorage numberingStorage;

  @Override
  public boolean allowGetNextNumber(final NodeRef nodeRef) {
    //Check for existance
    if (nodeRef == null || !nodeService.exists(nodeRef)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Node does not exist " + nodeRef);
      }
      return false;
    }

    //Check that the node is of allowed type or subtype
    boolean typeMatch = false;
    QName type = nodeService.getType(nodeRef);
    for (QName bindType : bindTypeQNames) {
      if (type.equals(bindType)) {
        typeMatch = true;
      } else if (dictionaryService.isSubClass(type, bindType)) {
        typeMatch = true;
      }
    }
    if (!typeMatch) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Node " + nodeRef + " of the type " + type.toString() + " is not in the list of allowed types ");
      }
      return false;
    }

    //Check if the node type is on the type ignore list
    for (QName ignoreType : ignoreTypeQNames) {
      if (type.equals(ignoreType)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Node " + nodeRef + " is on the list of ignored types " + type.toString());
        }
        return false;
      }
    }

    //Check if the node has an aspect which is not allowed
    Set<QName> aspects = nodeService.getAspects(nodeRef);
    for (QName ignoreAspect : ignoreAspectQNames) {
      if (aspects.contains(ignoreAspect)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Node " + nodeRef + " has aspect " + ignoreAspect.toString() + " which is not allowed");
        }
        return false;
      }
    }

    return true;
  }

  @Override
  public void assertAllowGetNextNumber(final NodeRef nodeRef) {
    if (!allowGetNextNumber(nodeRef)) {
      throw new AlfrescoRuntimeException(MSG_ERROR_NOT_ALLOWED);
    }
  }

  @Override
  public long getNextNumber(final NodeRef nodeRef) {
    assertAllowGetNextNumber(nodeRef);
    return numberingStorage.getNextNumber(startValue, id);
  }
  
  @Override
  public long getNextNumber(final NodeRef nodeRef,String subOptionValue) {
 //   assertAllowGetNextNumber(nodeRef,subOptionValue);
    return numberingStorage.getNextNumber(startValue, id,subOptionValue);
  }


 @Override
  public String getDecoratedNextNumber(final NodeRef nodeRef) {
    return decorator.decorate(getNextNumber(nodeRef), nodeRef);
  }
  
  @Override
  public String getDecoratedNextNumber(final NodeRef nodeRef,String subOptionValue) {
    return decorator.decorate(getNextNumber(nodeRef,subOptionValue), nodeRef);
  }

  public void setRepositoryHelper(Repository repositoryHelper) {
    this.repositoryHelper = repositoryHelper;
  }

  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setStartValue(long startValue) {
    this.startValue = startValue;
  }

  protected void checkForDictionaryExistance(List<String> bindTypes, List<QName> bindTypeQNames) {
    if (bindTypes != null && bindTypes.size() > 0) {
      for (String type : bindTypes) {
        QName typeQName = QName.resolveToQName(namespaceService, type);
        boolean found = false;
        TypeDefinition typeDef = dictionaryService.getType(typeQName);
        AspectDefinition aspectDef;
        if (typeDef == null) {
          aspectDef = dictionaryService.getAspect(typeQName);
          if (aspectDef != null) {
            found = true;
          }
        } else {
          found = true;
        }
        Assert.isTrue(found, "Type or aspect not registered in dictionary: " + type);
        bindTypeQNames.add(typeQName);
      }
    }
  }

  public void setBindTypes(List<String> bindTypes) {
    this.bindTypes = bindTypes;
    if (bindTypes != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Registering bind types: " + bindTypes.toString());
      }
      checkForDictionaryExistance(bindTypes, bindTypeQNames);
    }
  }

  public void setIgnoreAspects(List<String> ignoreAspects) {
    this.ignoreAspects = ignoreAspects;
    if (ignoreAspects != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Registering ignore aspects: " + ignoreAspects.toString());
      }
      checkForDictionaryExistance(ignoreAspects, ignoreAspectQNames);
    }
  }

  public void setIgnoreTypes(List<String> ignoreTypes) {
    this.ignoreTypes = ignoreTypes;
    if (ignoreTypes != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Registering ignore types: " + ignoreTypes.toString());
      }
      checkForDictionaryExistance(ignoreTypes, ignoreTypeQNames);
    }
  }

  public void setDecorator(Decorator decorator) {
    this.decorator = decorator;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public void setNumberingStorage(NumberingStorage numberingStorage) {
    this.numberingStorage = numberingStorage;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(repositoryHelper);
    Assert.notNull(nodeService);
    Assert.notNull(id);
    Assert.notNull(dictionaryService);
    Assert.notNull(namespaceService);

    Assert.notNull(numberingStorage);

  }

}
