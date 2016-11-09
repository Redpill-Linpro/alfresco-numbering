package org.redpill.alfresco.numbering.decorator;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Number prefixed with a date
 *
 * @author Marcus Svartmark
 */
public class PrefixDecorator extends BasicDecorator implements Decorator {

  protected String prefixSeparator = "";
  protected String prefix = "";

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setPrefixSeparator(String prefixSeparator) {
    this.prefixSeparator = prefixSeparator;
  }

  /**
   * Decorate a number with prefix and prefix separator
   * @param number
   * @param nodeRef
   * @param prefix
   * @return 
   */
  protected String decorate(long number, NodeRef nodeRef, String prefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(prefix);
    sb.append(prefixSeparator);
    sb.append(super.decorate(number, nodeRef));
    return sb.toString();
  }

  @Override
  public String decorate(long number, NodeRef nodeRef) {
    return decorate(number, nodeRef, prefix);
  }

}
