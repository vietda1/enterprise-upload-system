package vn.com.m1tech.cnn.appEtax.entities;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class TTINGIAYTO {

	@XmlElement(name = "GIAYTO", required = false)
	private List<GIAYTO> giayTo;

	public List<GIAYTO> getGiayTo() {
		return giayTo;
	}

	public void setGiayTo(List<GIAYTO> giayTo) {
		this.giayTo = giayTo;
	}

}
