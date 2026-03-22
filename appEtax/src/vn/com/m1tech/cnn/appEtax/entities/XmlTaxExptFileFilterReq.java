package vn.com.m1tech.cnn.appEtax.entities;

public class XmlTaxExptFileFilterReq extends PaginationReq {
	private String period;
	private String fileName;
	private String customerType;

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getCustomerType() {
		return customerType;
	}

	public void setCustomerType(String customerType) {
		this.customerType = customerType;
	}

}
