/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redpill.alfresco.numbering.decorator;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 *
 * @author mars
 */
public interface Decorator {

  /**
   * Format the number according a pattern
   *
   * @param number The counter number to decorate
   * @param nodeRef The noderef of the node that will eventually receive this
   * number (note that the decorator does not write the number)
   * @return
   */
  public String decorate(long number, NodeRef nodeRef);
}
