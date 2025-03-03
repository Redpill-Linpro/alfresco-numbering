package org.redpill.alfresco.numbering.storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Store numbers using node service. This is well tested and cluster safe.
 *
 * @author Marcus Svartmark - Redpill Linpro AB
 */
public class NodeNumberingStorageImpl implements NumberingStorage, InitializingBean {

  private static final String DD_XPATH = "/app:company_home/app:dictionary";
  protected static final String NUMBERING_FOLDER_NAME = "Numbering";
  private static final QName NUMBERING_FOLDER_QNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, NUMBERING_FOLDER_NAME);
  protected static final QName NUMBERING_PROPERTY = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "counterValue");
  private static final Logger LOG = LoggerFactory.getLogger(NodeNumberingStorageImpl.class);

  protected static long DEFAULT_LOCK_TTL = 30000L;
  protected ThreadLocal<String> lockThreadLocal = new ThreadLocal<String>();

  protected long lockTTL = DEFAULT_LOCK_TTL;

  protected JobLockService jobLockService;
  protected RetryingTransactionHelper retryingTransactionHelper;
  protected NamespaceService namespaceService;
  protected SearchService searchService;
  protected Repository repositoryHelper;
  protected NodeService nodeService;
  protected BehaviourFilter behaviourFilter;
  protected Map<String, NodeRef> counterCache = new HashMap<>();

  /**
   * Returns the node ref of the data dictionary
   *
   * @return NodeRef
   */
  protected NodeRef getDataDictionaryNode() {
    List<NodeRef> nodeRefList = searchService.selectNodes(repositoryHelper.getRootHome(), DD_XPATH, null, namespaceService, false);
    if (nodeRefList.size() != 1) {
      throw new AlfrescoRuntimeException("Could not look up data dictionary node");
    }
    return nodeRefList.get(0);
  }

  /**
   * Returns the node that is the holder for the counter properties
   *
   * @return NodeRef
   */
  protected NodeRef getCounterApp() {
    NodeRef counterFolderNodeRef = null;
    NodeRef dataDictionaryNode = getDataDictionaryNode();

    List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(dataDictionaryNode);
    for (ChildAssociationRef childAssoc : childAssocs) {
      Serializable property = nodeService.getProperty(childAssoc.getChildRef(), ContentModel.PROP_NAME);
      if (NUMBERING_FOLDER_NAME.equals(property)) {
        counterFolderNodeRef = childAssoc.getChildRef();
        break;
      }
    }

    if (counterFolderNodeRef == null) {
      counterFolderNodeRef = createCounterApp();
    }

    return counterFolderNodeRef;
  }

  /**
   * Create the counter app folder
   *
   * @return NodeRef
   */
  protected NodeRef createCounterApp() {
    String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
    AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
    ChildAssociationRef createNode = nodeService.createNode(getDataDictionaryNode(), ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, NUMBERING_FOLDER_NAME), ContentModel.TYPE_FOLDER);
    nodeService.setProperty(createNode.getChildRef(), ContentModel.PROP_NAME, NUMBERING_FOLDER_NAME);
    AuthenticationUtil.setFullyAuthenticatedUser(fullyAuthenticatedUser);
    return createNode.getChildRef();
  }

  /**
   * Get the counter node
   *
   * @param initialValue its initial value if node does not exist
   * @param id the id of the counter
   * @return NodeRef
   */
  protected NodeRef getCounterNode(final long initialValue, final String id) {
    NodeRef counterNodeRef = null;
    if (counterCache.containsKey(id)) {
      counterNodeRef = counterCache.get(id);
      //If it has been removed, the remove it from cache
      if (!nodeService.exists(counterNodeRef)) {
        counterCache.remove(id);
        counterNodeRef = null;
      }
    }
    if (counterNodeRef == null) {
      //The counter was not found in cache, try to fetch it fro mthe repo
      NodeRef counterAppFolderNodeRef = getCounterApp();
      List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(counterAppFolderNodeRef);
      QName expectedQname = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, id);
      for (ChildAssociationRef childAssoc : childAssocs) {
        Serializable property = nodeService.getProperty(childAssoc.getChildRef(), ContentModel.PROP_NAME);
        if (id.equals(property)) {
          counterNodeRef = childAssoc.getChildRef();
          break;
        } else {
          LOG.trace("Id: " + id + ", property:" + property);
        }
      }

      if (counterNodeRef == null) {
        counterNodeRef = createCounterNode(initialValue, id);
      }

      counterCache.put(id, counterNodeRef);
    }
    return counterNodeRef;
  }

  /**
   * Create the counter node
   *
   * @param initialValue its initial value if node does not exist
   * @param id the id of the counter
   * @return NodeRef
   */
  protected NodeRef createCounterNode(final long initialValue, final String id) {
    String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
    AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
    ChildAssociationRef createNode = nodeService.createNode(getCounterApp(), ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, id), ContentModel.TYPE_CONTENT);
    nodeService.setProperty(createNode.getChildRef(), ContentModel.PROP_NAME, id);
    nodeService.addAspect(createNode.getChildRef(), ContentModel.ASPECT_HIDDEN, null);
    nodeService.setProperty(createNode.getChildRef(), NUMBERING_PROPERTY, initialValue);
    AuthenticationUtil.setFullyAuthenticatedUser(fullyAuthenticatedUser);
    return createNode.getChildRef();
  }

  @Override
  public long getNextNumber(final long initialValue, final String id) {

    return retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Long>() {
      @Override
      public Long execute() throws Throwable {
        behaviourFilter.disableBehaviour();
        String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
        try {
          NodeRef counterNode = getCounterNode(initialValue, id);
          Long counterValue = (Long) nodeService.getProperty(counterNode, NUMBERING_PROPERTY);
          nodeService.setProperty(counterNode, NUMBERING_PROPERTY, ++counterValue);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Counter " + id + " increased to " + counterValue);
          }
          return counterValue;

        } finally {
          AuthenticationUtil.setFullyAuthenticatedUser(fullyAuthenticatedUser);
          behaviourFilter.enableBehaviour();
        }
      }
    }, false, true);

  }
  
  
  
  
  /**
   * Create the counter node based on optionValue
   *
   * @param initialValue its initial value if node does not exist
   * @param id the id of the counter
   * @return NodeRef
   */
  protected NodeRef createCounterNode(final long initialValue, final String id,final String optionValue) {
    String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
    AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
    ChildAssociationRef createNode = nodeService.createNode(getCounterApp(), ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, id), ContentModel.TYPE_CONTENT);
    nodeService.setProperty(createNode.getChildRef(), ContentModel.PROP_NAME, optionValue);
    nodeService.addAspect(createNode.getChildRef(), ContentModel.ASPECT_HIDDEN, null);
    nodeService.setProperty(createNode.getChildRef(), NUMBERING_PROPERTY, initialValue);
    AuthenticationUtil.setFullyAuthenticatedUser(fullyAuthenticatedUser);
    return createNode.getChildRef();
  }
  
  
  /**
   * Get the counter node based on optionValue
   *
   * @param initialValue its initial value if node does not exist
   * @param id the id of the counter
   * @return NodeRef
   */
  protected NodeRef getCounterNode(final long initialValue, final String id,final String optionValue) {
    NodeRef counterNodeRef = null;
    if (counterCache.containsKey(optionValue)) {
      counterNodeRef = counterCache.get(optionValue);
      //If it has been removed, the remove it from cache
      if (!nodeService.exists(counterNodeRef)) {
        counterCache.remove(optionValue);
        counterNodeRef = null;
      }
    }
    if (counterNodeRef == null) {
      //The counter was not found in cache, try to fetch it fro mthe repo
      NodeRef counterAppFolderNodeRef = getCounterApp();
      List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(counterAppFolderNodeRef);
      QName expectedQname = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, id);
      for (ChildAssociationRef childAssoc : childAssocs) {
        Serializable property = nodeService.getProperty(childAssoc.getChildRef(), ContentModel.PROP_NAME);
        if (optionValue.equals(property)) {
          counterNodeRef = childAssoc.getChildRef();
          break;
        } else {
          LOG.trace("Id: " + optionValue + ", property:" + property);
        }
      }

      if (counterNodeRef == null) {
        counterNodeRef = createCounterNode(initialValue, optionValue);
      }

      counterCache.put(optionValue, counterNodeRef);
    }
    return counterNodeRef;
  }
  
  
  @Override
  public long getNextNumber(final long initialValue, final String ids,final String optionValue) {

    return retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Long>() {
      @Override
      public Long execute() throws Throwable {
        behaviourFilter.disableBehaviour();
        String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
        try {
          NodeRef counterNode = getCounterNode(initialValue, optionValue,optionValue);
          Long counterValue = (Long) nodeService.getProperty(counterNode, NUMBERING_PROPERTY);
          nodeService.setProperty(counterNode, NUMBERING_PROPERTY, ++counterValue);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Counter " + optionValue + " increased to " + counterValue);
          }
          return counterValue;

        } finally {
          AuthenticationUtil.setFullyAuthenticatedUser(fullyAuthenticatedUser);
          behaviourFilter.enableBehaviour();
        }
      }
    }, false, false);

  }
  
  

  public void setJobLockService(JobLockService jobLockService) {
    this.jobLockService = jobLockService;
  }

  public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
    this.behaviourFilter = behaviourFilter;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setRepositoryHelper(Repository repositoryHelper) {
    this.repositoryHelper = repositoryHelper;
  }

  public void setRetryingTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
    this.retryingTransactionHelper = retryingTransactionHelper;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(jobLockService);
    Assert.notNull(behaviourFilter);
    Assert.notNull(namespaceService);
    Assert.notNull(nodeService);
    Assert.notNull(repositoryHelper);
    Assert.notNull(retryingTransactionHelper);
    Assert.notNull(searchService);
  }

}
