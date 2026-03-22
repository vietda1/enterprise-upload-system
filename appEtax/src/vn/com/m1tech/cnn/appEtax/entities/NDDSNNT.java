package vn.com.m1tech.cnn.appEtax.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class NDDSNNT {

	@XmlElement(name = "ND_DS_HDR", required = false)
	private NDDSHDR ndDsHdr;

	@XmlElement(name = "ND_DS_CTIET", required = false)
	private NDDSCTIET ndDsCTiet;

	public NDDSHDR getNdDsHdr() {
		return ndDsHdr;
	}

	public void setNdDsHdr(NDDSHDR ndDsHdr) {
		this.ndDsHdr = ndDsHdr;
	}

	public NDDSCTIET getNdDsCTiet() {
		return ndDsCTiet;
	}

	public void setNdDsCTiet(NDDSCTIET ndDsCTiet) {
		this.ndDsCTiet = ndDsCTiet;
	}

}
