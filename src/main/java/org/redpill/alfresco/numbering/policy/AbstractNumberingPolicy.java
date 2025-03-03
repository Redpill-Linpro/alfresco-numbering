package org.redpill.alfresco.numbering.policy;

import java.io.Serializable;
import java.util.Map;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnMoveNodePolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redpill.alfresco.numbering.component.NumberingComponent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public abstract class AbstractNumberingPolicy implements NumberingPolicy, OnAddAspectPolicy, OnMoveNodePolicy, InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractNumberingPolicy.class);

  protected PolicyComponent policyComponent;
  protected NodeService nodeService;
  protected DictionaryService dictionaryService;
  protected BehaviourFilter behaviourFilter;
  protected NumberingComponent numberingComponent;
  protected LockService lockService;
  protected Boolean isInitialized = false;
  protected QName typeQName;
  protected QName propertyQName;
  private final CopyBehaviourCallback copyBehaviourCallback = new DocumentNumberCopyBehaviourCallback();

  @Override
  public void register(QName typeQName, QName propertyQName) {
    this.typeQName = typeQName;
    this.propertyQName = propertyQName;
    if (!isInitialized()) {
      policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, typeQName, new JavaBehaviour(this, "onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));
      policyComponent.bindClassBehaviour(OnMoveNodePolicy.QNAME, typeQName, new JavaBehaviour(this, "onMoveNode", NotificationFrequency.TRANSACTION_COMMIT));
      policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"), typeQName, new JavaBehaviour(this, "getCopyCallback"));
    }
  }

  /**
   * Checks if the component is initalized. When called it will switch to being
   * initialized.
   *
   * @return true if already initalized, false otherwise
   */
  protected Boolean isInitialized() {
    if (isInitialized == false) {
      isInitialized = true;
    } else {
      return true;
    }
    return false;
  }

  /**
   * Checks if a node should be affected by this policy.
   *
   * @param nodeRef The node to test
   * @return true if the policy should be allowed to run, false if not
   */
  protected boolean allowUpdate(final NodeRef nodeRef) {
// Do not update nodes outside the workspace spacesstore
    if (!nodeRef.getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Tried to set document metadata on node (" + nodeRef + ") in store " + nodeRef.getStoreRef() + " which is ignored.");
      }
      return false;
    }

    if (!nodeService.exists(nodeRef)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node does not exist. Skipping...");
      }
      return false;
    }

    QName nodeType = nodeService.getType(nodeRef);
    if (ContentModel.TYPE_THUMBNAIL.equals(nodeType)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node type is cm:thumbnail. Skipping...");
      }
      return false;
    }

    if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node is working copy. Skipping...");
      }
      return false;
    }

    try {

      // Check that the node is not locked by another user
      lockService.checkForLock(nodeRef);

      // Find out if a node is within the document library. An exception will be
      // thrown if it is not within the document library.
    } catch (final NodeLockedException nle) {
      LOG.debug("Tried to set document metadata on locked node: " + nodeRef + " which is ignored.", nle);
      return false;
    } catch (Exception e) {
      LOG.warn("Unhandled exception in allowUpdate()", e);
      return false;
    }
    return true;
  }

  @Override
  public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(this.getClass().getName() + " onAddAspect begin, setting " + aspectTypeQName.getLocalName() + " aspect to " + nodeRef);
    }
    if (allowUpdate(nodeRef)) {
      setDocumentNumber(nodeRef, false);
    }
  }

  /**
   * Sets the document number on the node
   *
   * @param nodeRef The node
   * @param forceNewNumber Set to true to force a new number generation even if
   * the number already exist
   */
  protected void setDocumentNumber(NodeRef nodeRef, boolean forceNewNumber) {
    boolean enabled = behaviourFilter.isEnabled(nodeRef);
    if (enabled) {
      behaviourFilter.disableBehaviour(nodeRef);
    }
    String docNumber = (String) nodeService.getProperty(nodeRef, propertyQName);
    if (docNumber == null || docNumber.isEmpty() || forceNewNumber) {
      String decoratedNextNumber = numberingComponent.getDecoratedNextNumber(nodeRef);
      nodeService.setProperty(nodeRef, propertyQName, decoratedNextNumber);
      if (LOG.isTraceEnabled()) {
        LOG.trace("Setting document number to " + decoratedNextNumber + " for node " + nodeRef.toString());
      }
    }
    if (enabled) {
      behaviourFilter.enableBehaviour(nodeRef);
    }
  }

  @Override
  public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
    final NodeRef newNodeRef = newChildAssocRef.getChildRef();
    if (allowUpdate(newNodeRef)) {
      setDocumentNumber(newNodeRef, false);
    }
  }

  /**
   * The copy callback to use
   *
   * @return Returns the CopyBehaviourCallback
   */
  public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails) {
    return copyBehaviourCallback;
  }

  private class DocumentNumberCopyBehaviourCallback extends DefaultCopyBehaviourCallback {

    @Override
    public Map<QName, Serializable> getCopyProperties(
            QName classQName,
            CopyDetails copyDetails,
            Map<QName, Serializable> properties) {
      //Do not copy the document id, this should be reset instead!
      if (typeQName.equals(classQName)) {
        properties.remove(propertyQName);
      }
      return properties;
    }
  }

  /**
   * @param policyComponent the policyComponent to set
   */
  public void setPolicyComponent(PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  /**
   * @param nodeService the nodeService to set
   */
  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  /**
   * @param dictionaryService the dictionaryService to set
   */
  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  /**
   * @param behaviourFilter the behaviourFilter to set
   */
  public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
    this.behaviourFilter = behaviourFilter;
  }

  /**
   * @param numberingComponent the numberingComponent to set
   */
  public void setNumberingComponent(NumberingComponent numberingComponent) {
    this.numberingComponent = numberingComponent;
  }

  /**
   * @param lockService the lockService to set
   */
  public void setLockService(LockService lockService) {
    this.lockService = lockService;
  }
  
  
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(behaviourFilter);
    Assert.notNull(numberingComponent);
    Assert.notNull(lockService);
    Assert.notNull(dictionaryService);
    Assert.notNull(nodeService);
    Assert.notNull(policyComponent);
  }

}
