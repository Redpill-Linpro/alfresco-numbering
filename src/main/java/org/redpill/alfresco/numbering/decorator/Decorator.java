package org.redpill.alfresco.numbering.decorator;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 *
 * @author Marcus Svartmark - Redpill Linpro AB
 */
public interface Decorator {

  /**
   * Format the number according a pattern
   *
   * @param number The counter number to decorate (long)
   * @param nodeRef The noderef of the node that will eventually receive this
   * number (note that the decorator does not write the number)
   * @return String
   */
  public String decorate(long number, NodeRef nodeRef);
  
  /**
   * Format the number according a pattern
   *
   * @param number The counter number to decorate (String)
   * @param nodeRef The noderef of the node that will eventually receive this
   * number (note that the decorator does not write the number)
   * @return String
   */
  public String decorate(String number, NodeRef nodeRef);
}
