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
   * @param number The number to decorate
   * @param nodeRef The node this affects
   * @param prefix what prefix to append
   * @return String
   */
  protected String decorate(String number, NodeRef nodeRef, String prefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(prefix);
    sb.append(prefixSeparator);
    sb.append(super.decorate(number, nodeRef));
    return sb.toString();
  }
  
  @Override
  public String decorate(String number, NodeRef nodeRef) {
    return decorate(number, nodeRef, prefix);
  }
  
}
