package org.redpill.alfresco.numbering.storage;

/**
 * Storage interface to provide different means of numbering storage
 *
 * @author Marcus Svensson - Redpill Linpro AB
 */
public interface NumberingStorage {

  public static final String ATTR_ID = "RL_NUMBERING_COMPONENT";

  /**
   * Returns the next number in the numbering series.
   * @param initialValue The initial value of the counter
   * @param id The counter id
   * @return long
   */
  long getNextNumber(long initialValue, String id);
  
  /**
   * Returns the next number in the numbering series based on optionValue from drop down list
   * @param startValue
   * @param id
   * @param optionValue
   * @return
   */

  long getNextNumber(long startValue, String id, String optionValue);

}
