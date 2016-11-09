/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redpill.alfresco.numbering.component;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 *
 * @author mars
 */
public interface NumberingComponent {
  /**
   * Can be called to check if a new number generation is allowed. Will return
   * true or false to indicate if number generation is allowed or not.
   *
   * @param nodeRef
   * @return
   */
  public boolean allowGetNextNumber(final NodeRef nodeRef);
  
  /**
   * Can be called to check if a new number generation is allowed. Will throw
   * exception if not allowed
   *
   * @param nodeRef
   * @return
   */
  public void assertAllowGetNextNumber(final NodeRef nodeRef);
  
  /**
   * Get the next number in the sequence (increasing the sequence), will however
   * not store the increased number on the node
   *
   * @return
   */
  public long getNextNumber(final NodeRef nodeRef);
  
  /**
   * Get the next number in the sequence (increasing the sequence) and
   * decorating it according to the attached decorator, will however not store
   * the increased number on the node
   *
   * @param nodeRef
   * @return
   */
  public String getDecoratedNextNumber(final NodeRef nodeRef);
}
