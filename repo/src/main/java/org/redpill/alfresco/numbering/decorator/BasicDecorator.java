package org.redpill.alfresco.numbering.decorator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;

/**
 * A regular number series
 *
 * @author Marcus Svartmark
 */
public class BasicDecorator implements Decorator {

  protected int zeroPadding = 0;

  /**
   * Padding of zeros on the decorated number. A zero padding of 5 will give the
   * decorated number 00001 if the counter is at 1
   *
   * @param zeroPadding the number of zeros to add to the start
   */
  public void setZeroPadding(int zeroPadding) {
    this.zeroPadding = zeroPadding;
  }

  /**
   * Left pad number
   *
   * @param number The number to pad
   * @param padding The padding to use
   * @param padWith The character to pad with
   * @return
   */
  protected String leftPadNumber(String number, int padding, String padWith) {
    if (padding > 0) {
      return StringUtils.leftPad(number, padding, padWith);
    } else {
      return number;
    }
  }

  @Override
  public final String decorate(long number, NodeRef nodeRef) {
    return decorate(Long.toString(number), nodeRef);
  }

  @Override
  public String decorate(String number, NodeRef nodeRef) {
    return leftPadNumber(number, zeroPadding, "0");
  }

}
