package org.redpill.alfresco.numbering.component;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 *
 * @author Marcus Svartmark - Redpill Linpro AB
 */
public interface NumberingComponent {
  /**
   * Can be called to check if a new number generation is allowed. Will return
   * true or false to indicate if number generation is allowed or not.
   *
   * @param nodeRef The node to check
   * @return boolean
   */
  public boolean allowGetNextNumber(final NodeRef nodeRef);
  
  /**
   * Can be called to check if a new number generation is allowed. Will throw
   * exception if not allowed
   *
   * @param nodeRef The node to check
   */
  public void assertAllowGetNextNumber(final NodeRef nodeRef);
  
  /**
   * Get the next number in the sequence (increasing the sequence), will however
   * not store the increased number on the node
   *
   * @param nodeRef The node to check
   * @return long
   */
  public long getNextNumber(final NodeRef nodeRef);
  
  /**
   * Get the next number in the sequence (increasing the sequence) and
   * decorating it according to the attached decorator, will however not store
   * the increased number on the node
   *
   * @param nodeRef The node to get a decorated number for
   * @return String
   */
  public String getDecoratedNextNumber(final NodeRef nodeRef);
}
