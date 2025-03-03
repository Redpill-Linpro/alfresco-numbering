package org.redpill.alfresco.numbering.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class SampleNumberingPolicy extends AbstractNumberingPolicy implements InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(SampleNumberingPolicy.class);

  @Override
  protected boolean allowUpdate(final NodeRef nodeRef) {

    if (super.allowUpdate(nodeRef)) {
      QName nodeType = nodeService.getType(nodeRef);
      return dictionaryService.isSubClass(nodeType, ContentModel.TYPE_CONTENT);
    }
    return false;

  }

  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    register(ContentModel.TYPE_CONTENT, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "documentId"));
  }

}
