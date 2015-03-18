

/* First created by JCasGen Sun Feb 22 16:20:47 AST 2015 */
package qa.qcri.qf.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Feb 25 16:31:42 AST 2015
 * XML source: /home/alberto/Iyas/desc/Iyas/AcknowledgmentDescriptor.xml
 * @generated */
public class Acknowledgment extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Acknowledgment.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Acknowledgment() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Acknowledgment(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Acknowledgment(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Acknowledgment(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: ack

  /** getter for ack - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getAck() {
    if (Acknowledgment_Type.featOkTst && ((Acknowledgment_Type)jcasType).casFeat_ack == null)
      jcasType.jcas.throwFeatMissing("ack", "qa.qcri.qf.type.Acknowledgment");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Acknowledgment_Type)jcasType).casFeatCode_ack);}
    
  /** setter for ack - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAck(boolean v) {
    if (Acknowledgment_Type.featOkTst && ((Acknowledgment_Type)jcasType).casFeat_ack == null)
      jcasType.jcas.throwFeatMissing("ack", "qa.qcri.qf.type.Acknowledgment");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Acknowledgment_Type)jcasType).casFeatCode_ack, v);}    
  }

    