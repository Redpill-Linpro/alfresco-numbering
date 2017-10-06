
package org.redpill.alfresco.numbering.policy;

import org.alfresco.service.namespace.QName;

public interface NumberingPolicy {
  /**
   * Register the policy
   * @param typeQName The type or aspect to listen for
   * @param propertyQName The document numbering property to use
   */
  public void register(QName typeQName, QName propertyQName);
  
}
