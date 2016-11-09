package org.redpill.alfresco.numbering.decorator;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Number prefixed with a date
 *
 * @author Marcus Svartmark
 */
public class CurrentDatePrefixDecorator extends PrefixDecorator implements Decorator {

  protected String datePattern = "yyyy-MM-dd";

  public void setDatePattern(String datePattern) {
    this.datePattern = datePattern;
  }

  @Override
  public String decorate(long number, NodeRef nodeRef) {
    SimpleDateFormat df = new SimpleDateFormat(datePattern);
    return decorate(number, nodeRef, df.format(new Date()));
  }

}
