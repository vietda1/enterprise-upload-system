package vn.com.m1tech.cnn.appEtax.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "GIAODICH")
@XmlAccessorType(XmlAccessType.FIELD)
public class GIAODICH {

	@XmlElement(name = "ND_DS_NNT", required = false)
	private NDDSNNT ndDsNnt;

	public NDDSNNT getNdDsNnt() {
		return ndDsNnt;
	}

	public void setNdDsNnt(NDDSNNT ndDsNnt) { 
		this.ndDsNnt = ndDsNnt;
	}

}
