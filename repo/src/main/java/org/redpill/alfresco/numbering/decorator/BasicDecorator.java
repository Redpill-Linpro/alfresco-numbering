package org.redpill.alfresco.numbering.decorator;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * A regular number series
 *
 * @author Marcus Svartmark
 */
public class BasicDecorator implements Decorator {

  protected long zeroPadding = 0;

  /**
   * Padding of zeros on the decorated number. A zero padding of 5 will give the
   * decorated number 00001 if the counter is at 1
   *
   * @param zeroPadding
   */
  public void setZeroPadding(long zeroPadding) {
    this.zeroPadding = zeroPadding;
  }

  @Override
  public String decorate(long number, NodeRef nodeRef) {
    return Long.toString(number);
  }

}
