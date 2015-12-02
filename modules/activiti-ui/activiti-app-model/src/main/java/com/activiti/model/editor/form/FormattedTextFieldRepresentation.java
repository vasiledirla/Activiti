package com.activiti.model.editor.form;

public class FormattedTextFieldRepresentation extends FormFieldRepresentation {

  private String formatterType;
  private String acceptedChars;
  private String format;
  private Integer decimalsCount;
  private Boolean allowNegatives;

  public Integer getDecimalsCount() {
    return decimalsCount;
  }
  public void setDecimalsCount(Integer decimalsCount) {
    this.decimalsCount = decimalsCount;
  }
  public Boolean getAllowNegatives() {
    return allowNegatives;
  }
  public void setAllowNegatives(Boolean allowNegatives) {
    this.allowNegatives = allowNegatives;
  }
  public String getFormatterType() {
    return formatterType;
  }
  public void setFormatterType(String formatterType) {
    this.formatterType = formatterType;
  }

  public String getAcceptedChars() {
    return acceptedChars;
  }
  public void setAcceptedChars(String acceptedChars) {
    this.acceptedChars = acceptedChars;
  }
  public String getFormat() {
    return format;
  }
  public void setFormat(String format) {
    this.format = format;
  }
}